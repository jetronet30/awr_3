let intervalIds = new Set(); // Store setTimeout IDs
let eventListeners = new Map(); // Store event listeners for cleanup

// Helper function to track event listeners
function trackEventListener(element, event, handler) {
    eventListeners.set(element, [...(eventListeners.get(element) || []), { event, handler }]);
    element.addEventListener(event, handler);
}

// Helper function to clear all tracked event listeners
function clearEventListeners() {
    eventListeners.forEach((listeners, element) => {
        listeners.forEach(({ event, handler }) => {
            element.removeEventListener(event, handler);
        });
    });
    eventListeners.clear();
}

// Function to update progress bar
function updateProgressBar(progress) {
    const progressBar = document.querySelector("#wagontares-progressBar");
    if (progressBar) {
        progressBar.value = progress;
    }
}

export function initWagontaresModule() {
    const netContainer = document.querySelector("#wagontares-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) {
        return;
    }

    function updateIndicator(success) {
        const indicator = document.querySelector("#wagontares-indicator");
        if (indicator) {
            indicator.style.transition = "background-color 0.5s ease";
            indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";
            const timeoutId = setTimeout(() => {
                indicator.style.backgroundColor = "";
            }, 5000);
            intervalIds.add(timeoutId); // Track timeout
        }
    }

    /** --- 🛰 addTare ფორმა --- */
    const setForm = netContainer.querySelector('form[action="/addwagonTare"]');
    const setBtn = setForm?.querySelector("#wagontares-add-tare-btn");

    if (setForm && setBtn) {
        setBtn.replaceWith(setBtn.cloneNode(true));
        const newSetBtn = netContainer.querySelector("#wagontares-add-tare-btn");

        const handler = async (e) => {
            e.preventDefault();
            const formData = new FormData(setForm);
            try {
                const response = await fetch(setForm.action, { method: "POST", body: formData });
                if (!response.ok) throw new Error(`Server error: ${response.status}`);
                const html = await response.text();
                content.innerHTML = html;
                if (content.querySelector("#wagontares-container")) {
                    const mod = await import(`/scripts/wagontares.js?v=${Date.now()}`);
                    mod.initWagontaresModule();
                }
                updateIndicator(true);
            } catch (err) {
                content.innerHTML = `<p style="color:red;">Error applying wagontares.</p>`;
                updateIndicator(false);
                alert("შეცდომა wagontares პარამეტრების გამოყენებისას: " + err.message);
            }
        };
        trackEventListener(newSetBtn, "click", handler);
    }


    /** --- ⚙️ tarewagons ფორმები --- */
    const lanSettingForms = netContainer.querySelectorAll(".net-set-from");
    lanSettingForms.forEach(form => {
        const setBtn = form.querySelector(".net-set-btn");
        if (setBtn) {
            setBtn.replaceWith(setBtn.cloneNode(true));
            const newSetBtn = form.querySelector(".net-set-btn");

            const handler = async (e) => {
                e.preventDefault();
                const formData = new FormData(form);
                try {
                    const response = await fetch(form.action, { method: "POST", body: formData });
                    if (!response.ok) throw new Error(`Server error: ${response.status}`);
                    const result = await response.json();
                    updateIndicator(result.success);
                    if (!result.success && result.message) {
                        alert("LAN პარამეტრების განახლება ჩაიშალა: " + result.message);
                    } else if (!result.success) {
                        alert("LAN პარამეტრების განახლება ჩაიშალა: უცნობი შეცდომა");
                    }
                } catch (err) {
                    content.innerHTML = `<p style="color:red;">Error applying LAN settings.</p>`;
                    updateIndicator(false);
                    alert("შეცდომა LAN პარამეტრების განახლებისას: " + err.message);
                }
            };
            trackEventListener(newSetBtn, "click", handler);
        }
    });

    /** --- 🌐 upload backup --- */
    const uploadForm = netContainer.querySelector('form[action="/uploaTareBaseMDB"]');
    const uploadBtn = uploadForm?.querySelector('input[type="button"]');
    const fileInput = uploadForm?.querySelector('input[type="file"]');

    if (uploadForm && uploadBtn && fileInput) {
        uploadBtn.replaceWith(uploadBtn.cloneNode(true));
        const newUploadBtn = uploadForm.querySelector('input[type="button"]');

        const handler = async (e) => {
            e.preventDefault();
            const file = fileInput.files[0];
            if (!file) {
                alert("გთხოვთ, აირჩიეთ ფაილი!");
                return;
            }

            // Check file size (100GB = 100 * 1024 * 1024 * 1024 bytes)
            const maxSize = 100 * 1024 * 1024 * 1024;
            if (file.size > maxSize) {
                alert("ფაილი ძალიან დიდია! მაქსიმალური ზომაა 20GB.");
                return;
            }

            const formData = new FormData(uploadForm);
            try {
                // Use XMLHttpRequest for progress tracking
                const xhr = new XMLHttpRequest();

                // Progress event listener
                xhr.upload.addEventListener("progress", (event) => {
                    if (event.lengthComputable) {
                        const percentComplete = (event.loaded / event.total) * 100;
                        updateProgressBar(percentComplete);
                    }
                });

                // Completion handler
                xhr.onload = () => {
                    if (xhr.status === 200) {
                        content.innerHTML = xhr.responseText;
                        if (content.querySelector("#wagontares-container")) {
                            import(`/scripts/wagontares.js?v=${Date.now()}`)
                                .then(mod => mod.initWagontaresModule());
                        }
                        updateIndicator(true);
                        updateProgressBar(0); // Reset progress bar
                    } else {
                        throw new Error(`Server error: ${xhr.status}`);
                    }
                };

                // Error handler
                xhr.onerror = () => {
                    content.innerHTML = `<p style="color:red;">Error uploading backup.</p>`;
                    updateIndicator(false);
                    updateProgressBar(0); // Reset progress bar
                    alert("შეცდომა ფაილის ატვირთვისას.");
                };

                xhr.open("POST", uploadForm.action, true);
                xhr.send(formData);
            } catch (err) {
                content.innerHTML = `<p style="color:red;">Error uploading backup.</p>`;
                updateIndicator(false);
                updateProgressBar(0); // Reset progress bar
                alert("შეცდომა ფაილის ატვირთვისას: " + err.message);
            }
        };
        trackEventListener(newUploadBtn, "click", handler);
    }
}

export function cleanupWagontaresModule() {
    // Clear all setTimeouts
    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    // Clear all event listeners
    clearEventListeners();

    // Reset progress bar
    updateProgressBar(0);

    console.log('✅ wgontares module cleaned up');
}