let intervalIds = new Set();     // setTimeout-ების შესანახად
let eventListeners = new Map();  // event listener-ების თვალყურის დევნება cleanup-ისთვის

// დამხმარე ფუნქცია event-ების თვალყურის დევნებისთვის
function trackEventListener(element, event, handler) {
    if (!eventListeners.has(element)) {
        eventListeners.set(element, []);
    }
    eventListeners.get(element).push({ event, handler });
    element.addEventListener(event, handler);
}

// ყველა თვალყურადევნებული listener-ის მოხსნა
function clearEventListeners() {
    eventListeners.forEach((listeners, element) => {
        listeners.forEach(({ event, handler }) => {
            element.removeEventListener(event, handler);
        });
    });
    eventListeners.clear();
}

// პროგრეს ბარის განახლების ფუნქცია (ორივესთვის ერთიანი)
function updateProgressBar(barId, progress) {
    const progressBar = document.getElementById(barId);
    if (progressBar) {
        progressBar.value = progress;
    }
}

export function initOcrModule() {
    const ocrContainer = document.querySelector("#ocr-settings-container");
    const content = document.querySelector("main.content");

    if (!ocrContainer || !content) return;

    function updateIndicator(success) {
        const indicator = document.querySelector("#ocr-settings-indicator");
        if (!indicator) return;

        indicator.style.transition = "background-color 0.5s ease";
        indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";

        const timeoutId = setTimeout(() => {
            indicator.style.backgroundColor = "";
        }, 5000);

        intervalIds.add(timeoutId);
    }

    // ───── Reboot ღილაკი ─────
    const rebootForm = ocrContainer.querySelector('form[action="/ocr-setting-reboot"]');
    let rebootBtn = rebootForm?.querySelector("#ocr-settings-reboot-btn");

    if (rebootForm && rebootBtn) {
        const newRebootBtn = rebootBtn.cloneNode(true);
        rebootBtn.replaceWith(newRebootBtn);

        const handler = async (e) => {
            e.preventDefault();
            const formData = new FormData(rebootForm);

            try {
                const response = await fetch(rebootForm.action, {
                    method: "POST",
                    body: formData
                });

                if (!response.ok) throw new Error(`Server error: ${response.status}`);

                const html = await response.text();
                content.innerHTML = html;

                if (content.querySelector("#ocr-settings-container")) {
                    const mod = await import(`/scripts/ocr.js?v=${Date.now()}`);
                    mod.initOcrModule();
                }

                updateIndicator(true);
            } catch (err) {
                content.innerHTML = `<p style="color:red;">OCR reboot-ის გამოყენების შეცდომა</p>`;
                updateIndicator(false);
                alert("შეცდომა OCR reboot-ისას:\n" + err.message);
            }
        };

        trackEventListener(newRebootBtn, "click", handler);
    }

    // ───── OCR პარამეტრების ფორმები ─────
    const ocrForms = ocrContainer.querySelectorAll(".ocr-settings-from");

    ocrForms.forEach(form => {
        const setBtn = form.querySelector(".ocr-settings-btn");
        if (!setBtn) return;

        const newSetBtn = setBtn.cloneNode(true);
        setBtn.replaceWith(newSetBtn);

        const handler = async (e) => {
            e.preventDefault();
            const formData = new FormData(form);

            try {
                const response = await fetch(form.action, {
                    method: "POST",
                    body: formData
                });

                if (!response.ok) throw new Error(`Server responded: ${response.status}`);

                const result = await response.json();
                updateIndicator(result.success ?? false);

                if (!result.success) {
                    const msg = result.message || "უცნობი შეცდომა";
                    alert("OCR პარამეტრების განახლება ჩაიშალა:\n" + msg);
                }
            } catch (err) {
                updateIndicator(false);
                alert("შეცდომა OCR პარამეტრების განახლებისას:\n" + err.message);
            }
        };

        trackEventListener(newSetBtn, "click", handler);
    });

    // ───── YOLO მოდელის ატვირთვა ─────
    const yoloForm = ocrContainer.querySelector('#yolo-uplad-form');
    const yoloBtn = yoloForm?.querySelector('#yolo-upload-btn');
    const yoloFileInput = yoloForm?.querySelector('input[type="file"]');

    if (yoloForm && yoloBtn && yoloFileInput) {
        const newYoloBtn = yoloBtn.cloneNode(true);
        yoloBtn.replaceWith(newYoloBtn);

        const handler = (e) => {
            e.preventDefault();
            const file = yoloFileInput.files[0];
            if (!file) {
                alert("გთხოვთ, აირჩიოთ YOLO მოდელის ფაილი!");
                return;
            }

            // სურვილისამებრ: ზომის შეზღუდვა (მაგ. 4GB)
            const maxSize = 4 * 1024 * 1024 * 1024;
            if (file.size > maxSize) {
                alert("ფაილი ძალიან დიდია! მაქს. ზომა: 4GB");
                return;
            }

            const formData = new FormData(yoloForm);

            const xhr = new XMLHttpRequest();

            xhr.upload.addEventListener("progress", (event) => {
                if (event.lengthComputable) {
                    const percent = Math.round((event.loaded / event.total) * 100);
                    updateProgressBar("yolo-update-progressBar", percent);
                }
            });

            xhr.onload = () => {
                if (xhr.status === 200) {
                    // თუ სერვერი HTML-ს აბრუნებს → გვერდის განახლება
                    content.innerHTML = xhr.responseText;
                    if (content.querySelector("#ocr-settings-container")) {
                        import(`/scripts/ocr.js?v=${Date.now()}`)
                            .then(mod => mod.initOcrModule());
                    }
                    updateIndicator(true);
                    updateProgressBar("yolo-update-progressBar", 0);
                } else {
                    updateIndicator(false);
                    updateProgressBar("yolo-update-progressBar", 0);
                    alert(`YOLO ატვირთვა ჩაიშალა (status ${xhr.status})`);
                }
            };

            xhr.onerror = () => {
                updateIndicator(false);
                updateProgressBar("yolo-update-progressBar", 0);
                alert("შეცდომა YOLO ფაილის ატვირთვისას (ქსელური პრობლემა)");
            };

            xhr.open("POST", yoloForm.action, true);
            xhr.send(formData);
        };

        trackEventListener(newYoloBtn, "click", handler);
    }

    // ───── TROCR მოდელის ატვირთვა ─────
    const trocrForm = ocrContainer.querySelector('#trocr-uplad-form');
    const trocrBtn = trocrForm?.querySelector('#trocr-upload-btn');
    const trocrFileInput = trocrForm?.querySelector('input[type="file"]');

    if (trocrForm && trocrBtn && trocrFileInput) {
        const newTrocrBtn = trocrBtn.cloneNode(true);
        trocrBtn.replaceWith(newTrocrBtn);

        const handler = (e) => {
            e.preventDefault();
            const file = trocrFileInput.files[0];
            if (!file) {
                alert("გთხოვთ, აირჩიოთ TROCR მოდელის ფაილი!");
                return;
            }

            // იგივე ლიმიტი (შეგიძლია შეცვალო)
            const maxSize = 4 * 1024 * 1024 * 1024;
            if (file.size > maxSize) {
                alert("ფაილი ძალიან დიდია! მაქს. ზომა: 4GB");
                return;
            }

            const formData = new FormData(trocrForm);

            const xhr = new XMLHttpRequest();

            xhr.upload.addEventListener("progress", (event) => {
                if (event.lengthComputable) {
                    const percent = Math.round((event.loaded / event.total) * 100);
                    updateProgressBar("trocr-update-progressBar", percent);
                }
            });

            xhr.onload = () => {
                if (xhr.status === 200) {
                    content.innerHTML = xhr.responseText;
                    if (content.querySelector("#ocr-settings-container")) {
                        import(`/scripts/ocr.js?v=${Date.now()}`)
                            .then(mod => mod.initOcrModule());
                    }
                    updateIndicator(true);
                    updateProgressBar("trocr-update-progressBar", 0);
                } else {
                    updateIndicator(false);
                    updateProgressBar("trocr-update-progressBar", 0);
                    alert(`TROCR ატვირთვა ჩაიშალა (status ${xhr.status})`);
                }
            };

            xhr.onerror = () => {
                updateIndicator(false);
                updateProgressBar("trocr-update-progressBar", 0);
                alert("შეცდომა TROCR ფაილის ატვირთვისას (ქსელური პრობლემა)");
            };

            xhr.open("POST", trocrForm.action, true);
            xhr.send(formData);
        };

        trackEventListener(newTrocrBtn, "click", handler);
    }
}

export function cleanupOcrModule() {
    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();
    clearEventListeners();

    // პროგრეს ბარების განულება
    updateProgressBar("yolo-update-progressBar", 0);
    updateProgressBar("trocr-update-progressBar", 0);

    console.log('OCR module cleaned up');
}