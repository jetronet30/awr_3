/* ============================== scale5.js ============================== */
// მოდული #scale5-container-ისთვის (TCP_3) — უსაფრთხო, განახლებული ვერსია
/* ====================================================================== */

let intervalIds = new Set();
let eventListeners = new Map();
let hlsInstanceScale5 = null;
let videoObserver = null;
let weightEventSource = null;

// === ინიციალიზაციის ფლაგი (მხოლოდ ერთხელ) ===
let isScale5Initialized = false;

/* ---------- Event-listener მართვა ---------- */
function trackEventListener(element, event, handler) {
    if (!element) return;
    const list = eventListeners.get(element) || [];
    eventListeners.set(element, [...list, { event, handler }]);
    element.addEventListener(event, handler);
}

function clearEventListeners() {
    eventListeners.forEach((listeners, element) => {
        listeners.forEach(({ event, handler }) => {
            element.removeEventListener(event, handler);
        });
    });
    eventListeners.clear();
}

/* ---------- Helper: წაიკითხავს #magonNumLeght_4-ს → 8, 10, 12 (default: 8) ---------- */
function getAllowedWagonLength(netContainer) {
    const input = netContainer.querySelector("#magonNumLeght_5");
    const value = input?.value?.trim();
    const num = parseInt(value, 10);
    return (Number.isInteger(num) && [8, 10, 12].includes(num)) ? num : 8;
}

/* ---------- საწყისი ვაგონების ჩატვირთვა ---------- */
async function loadInitialWagonData(netContainer, updateIndicator) {
    if (!netContainer) return;

    const targetContainer = netContainer.querySelector("#operation-data5-container");
    if (!targetContainer) return;

    try {
        const response = await fetch("/showweighingWagons5", { method: "POST" });
        if (!response.ok) throw new Error(`Initial load failed: ${response.status}`);

        const html = await response.text();
        targetContainer.innerHTML = html;

        updateIndicator?.(true);
        bindEditWagonForm(netContainer, updateIndicator);
    } catch (err) {
        targetContainer.innerHTML = `<p style="color:red;">ჩატვირთვის შეცდომა: ${err.message}</p>`;
        updateIndicator?.(false);
    }
}

/* ---------- რედაქტირების ფორმების დაკავშირება + ავტომატური გაგზავნა ---------- */
function bindEditWagonForm(netContainer, updateIndicator) {
    const editForms = netContainer.querySelectorAll('form.oprdata5-set-from');
    const allowedLength = getAllowedWagonLength(netContainer);

    editForms.forEach((editForm) => {
        const editBtn = editForm.querySelector('.oprdata5-set-btn');
        const wagonNumberInput = editForm.querySelector('.scale5-operdata-wagonNum-input');

        if (!editBtn || !wagonNumberInput) return;

        const clonedBtn = editBtn.cloneNode(true);
        editBtn.replaceWith(clonedBtn);

        const submitHandler = async (e) => {
            e?.preventDefault();

            const formData = new FormData(editForm);
            const actionUrl = editForm.getAttribute("action");

            if (!actionUrl) return;

            try {
                const response = await fetch(actionUrl, { method: "POST", body: formData });
                if (!response.ok) throw new Error(`Server error: ${response.status}`);
                const result = await response.json();

                if (result.success) {
                    editForm.style.backgroundColor = "#1da81dff";
                    setTimeout(() => editForm.style.backgroundColor = "", 2000);
                } else {
                    editForm.style.backgroundColor = "#fc0a1eff";
                    setTimeout(() => editForm.style.backgroundColor = "", 3000);
                    alert(`ვაგონი ${result.message} - განახლება ვერ მოხერხდა.`);
                }
            } catch (err) {
                updateIndicator?.(false);
                alert(`შეცდომა: ${err.message}`);
            }
        };

        trackEventListener(clonedBtn, "click", submitHandler);

        let isSubmitting = false;
        const autoSubmitHandler = () => {
            const value = wagonNumberInput.value.trim();
            if (value.length === allowedLength && !isSubmitting) {
                isSubmitting = true;
                submitHandler();
                setTimeout(() => { isSubmitting = false; }, 1000);
            }
        };

        trackEventListener(wagonNumberInput, "input", autoSubmitHandler);
        trackEventListener(wagonNumberInput, "keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                const len = wagonNumberInput.value.trim().length;
                if (len === allowedLength || len > 0) submitHandler();
            }
        });
    });
}

/* ---------- ვიდეოს HLS ინიციალიზაცია ---------- */
function initVideoScale5() {
    const video = document.getElementById('player-5');
    if (!video || !video.getAttribute('data-hls-src')) return;

    if (hlsInstanceScale5) {
        hlsInstanceScale5.destroy();
        hlsInstanceScale5 = null;
    }

    if (Hls.isSupported()) {
        hlsInstanceScale5 = new Hls({
            maxBufferLength: 15,
            maxMaxBufferLength: 20,
            maxBufferSize: 20 * 1000 * 1000,
            liveSyncDurationCount: 3,
            liveMaxLatencyDurationCount: 8,
            xhrSetup: (xhr) => { xhr.timeout = 10000; },
        });

        hlsInstanceScale5.loadSource(video.getAttribute('data-hls-src'));
        hlsInstanceScale5.attachMedia(video);

        hlsInstanceScale5.on(Hls.Events.MANIFEST_PARSED, () => video.play().catch(() => {}));
        hlsInstanceScale5.on(Hls.Events.ERROR, (event, data) => {
            if (data.fatal || data.details === 'levelLoadError') {
                hlsInstanceScale5.destroy();
                hlsInstanceScale5 = null;
                setTimeout(initVideoScale5, 2000);
            }
        });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = video.getAttribute('data-hls-src');
        video.play().catch(() => {});
    }
}

function observeVideoScale5(container) {
    if (videoObserver) videoObserver.disconnect();

    videoObserver = new MutationObserver(() => {
        const video = document.getElementById('player-5');
        if (video && !hlsInstanceScale5 && video.getAttribute('data-hls-src')) {
            initVideoScale5();
        }
    });

    videoObserver.observe(container, { childList: true, subtree: true });
}

/* ---------- SSE CONNECTION: /sendscale4 → TCP_3 → #w-indic-5 ---------- */
function connectWeightSSE(netContainer, updateIndicator) {
    if (weightEventSource) {
        weightEventSource.close();
        weightEventSource = null;
    }

    weightEventSource = new EventSource('/sendscale5');

    weightEventSource.addEventListener('TCP_3', (e) => {
        const data = e.data.trim();

        if (data === 'update-data-container') {
            loadInitialWagonData(netContainer, updateIndicator);
            return;
        }

        const weightInput = document.getElementById('w-indic-5');

        if (data === 'update-data-works-start') {
            weightInput.value = "START"
            weightInput.style.color = '#fdec04ff';
            return;
        }

        if (data === 'update-data-works-stop') {
            weightInput.value = "END"
            weightInput.style.color = '#fd048dff';
            return;
        }

        if (weightInput) {
            weightInput.value = data;
            weightInput.style.color = '#04cbfdff';
            setTimeout(() => { weightInput.style.color = ''; }, 1000);
        }
    });

    weightEventSource.onerror = () => {
        weightEventSource.close();
        setTimeout(() => connectWeightSSE(netContainer, updateIndicator), 2000);
    };

    weightEventSource.onopen = () => {
        console.log('SSE connected: /sendscale5 (TCP_3)');
    };
}

/* ---------- მოდულის ინიციალიზაცია — მხოლოდ ერთხელ! ---------- */
export function initScale5Module() {
    if (isScale5Initialized) {
        console.warn("scale5 უკვე ინიციალიზებულია. გამოტოვება.");
        return;
    }

    isScale5Initialized = true;
    const netContainer = document.querySelector("#scale5-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) {
        isScale5Initialized = false;
        return;
    }

    function updateIndicator(success) {
        const indicator = document.querySelector("#scale5-indicator");
        if (indicator) {
            indicator.style.transition = "background-color 0.5s ease";
            indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";
            const id = setTimeout(() => indicator.style.backgroundColor = "", 5000);
            intervalIds.add(id);
        }
    }

    // 1. Load initial data
    loadInitialWagonData(netContainer, updateIndicator);
    connectWeightSSE(netContainer, updateIndicator);

    // 2. Helper: Reload module safely
    const reloadModule = async (actionUrl) => {
        cleanupScale5Module(); // სრული გასუფთავება

        try {
            const response = await fetch(actionUrl, { method: "POST" });
            if (!response.ok) throw new Error(`Status: ${response.status}`);
            const html = await response.text();
            content.innerHTML = html;

            if (content.querySelector("#scale5-container")) {
                const mod = await import(`/scripts/scale5.js?v=${Date.now()}`);
                mod.initScale5Module();
            }
            updateIndicator(true);
        } catch (err) {
            content.innerHTML = `<p style="color:red;">Error: ${err.message}</p>`;
            updateIndicator(false);
        }
    };

    // 3. START WEIGHING
    const startForm = netContainer.querySelector('form[action="/startWeighing_5"]');
    const startBtn = startForm?.querySelector("#scale5-start-weighing-btn");
    if (startForm && startBtn) {
        const newBtn = startBtn.cloneNode(true);
        startBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(startForm.action);
        });
    }

    // 4. ABORT WEIGHING
    const abortForm = netContainer.querySelector('form[action="/abortWeighing_5"]');
    const abortBtn = abortForm?.querySelector("#scale5-abort-weighing-btn");
    if (abortForm && abortBtn) {
        const newBtn = abortBtn.cloneNode(true);
        abortBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(abortForm.action);
        });
    }

    // 5. ADD WAGON + AUTO-SUBMIT
    const addForm = netContainer.querySelector('form[action="/addwagonWeighing_5"]');
    const addBtn = addForm?.querySelector("#scale5-add-wagon-btn");
    const wagonNumberInput = addForm?.querySelector("#scale5-number-input");

    if (addForm && addBtn && wagonNumberInput) {
        const newBtn = addBtn.cloneNode(true);
        addBtn.replaceWith(newBtn);

        const allowedLength = getAllowedWagonLength(netContainer);
        let isSubmitting = false;

        const submitHandler = async (e) => {
            if (e) e.preventDefault();
            if (isSubmitting) return;
            isSubmitting = true;

            const targetContainer = netContainer.querySelector("#operation-data5-container");
            if (!targetContainer) return;

            try {
                const response = await fetch(addForm.action, {
                    method: "POST",
                    body: new FormData(addForm)
                });
                if (!response.ok) throw new Error(`Status: ${response.status}`);

                const html = await response.text();
                targetContainer.innerHTML = html;
                updateIndicator(true);

                ['#scale5-number-input', '#scale5-product-input', '#scale5-count-input'].forEach(sel => {
                    const input = addForm.querySelector(sel);
                    if (input) input.value = '';
                });

                bindEditWagonForm(netContainer, updateIndicator);
            } catch (err) {
                targetContainer.innerHTML = `<p style="color:red;">შეცდომა: ${err.message}</p>`;
                updateIndicator(false);
            } finally {
                setTimeout(() => { isSubmitting = false; }, 1000);
            }
        };

        trackEventListener(newBtn, "click", submitHandler);

        const autoSubmitHandler = () => {
            const len = wagonNumberInput.value.trim().length;
            if (len === allowedLength && !isSubmitting) submitHandler();
        };

        trackEventListener(wagonNumberInput, "input", autoSubmitHandler);
        trackEventListener(wagonNumberInput, "keydown", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                const len = wagonNumberInput.value.trim().length;
                if (len === allowedLength || len > 0) submitHandler();
            }
        });
    }

    // 6. UPDATE ALL WAGONS
    const updateForm = netContainer.querySelector('form[action="/updateAllWeighing_5"]');
    const updateBtn = updateForm?.querySelector("#scale5-update-weighing-btn");

    if (updateForm && updateBtn) {
        const newBtn = updateBtn.cloneNode(true);
        updateBtn.replaceWith(newBtn);

        trackEventListener(newBtn, "click", async (e) => {
            e.preventDefault();
            const targetContainer = netContainer.querySelector("#operation-data5-container");
            if (!targetContainer) return;

            try {
                const response = await fetch(updateForm.action, { method: "POST" });
                if (!response.ok) throw new Error(`Status: ${response.status}`);
                const html = await response.text();
                targetContainer.innerHTML = html;
                updateIndicator(true);
                bindEditWagonForm(netContainer, updateIndicator);
            } catch (err) {
                targetContainer.innerHTML = `<p style="color:red;">შეცდომა: ${err.message}</p>`;
                updateIndicator(false);
            }
        });
    }

    // 7. VIDEO
    initVideoScale5();
    observeVideoScale5(netContainer);

    // 8. Global cleanup access
    window.cleanupScale5Module = cleanupScale5Module;
}

/* ---------- სრული გასუფთავება — ყოველ ჯერზე გამოიძახება ---------- */
export function cleanupScale5Module() {
    console.log('Cleaning up scale5 module...');

    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    clearEventListeners();

    if (hlsInstanceScale5) {
        hlsInstanceScale5.destroy();
        hlsInstanceScale5 = null;
    }

    if (videoObserver) {
        videoObserver.disconnect();
        videoObserver = null;
    }

    if (weightEventSource) {
        weightEventSource.close();
        weightEventSource = null;
    }

    isScale5Initialized = false;

    console.log('scale5 module fully cleaned');
}

// Auto-cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (typeof cleanupScale5Module === 'function') {
        cleanupScale5Module();
    }
});