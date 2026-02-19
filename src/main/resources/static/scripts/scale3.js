// =============================================================================
// scale3.js — მოდული #scale3-container-ისთვის (განახლებული 2025/2026)
// =============================================================================

let intervalIds = new Set();
let eventListeners = new Map();
let hlsInstanceScale3 = null;
let videoObserver = null;
let weightEventSource = null;

// === ინიციალიზაციის ფლაგი (მხოლოდ ერთხელ) ===
let isScale3Initialized = false;

// =============================================================================
// Helper: Event Listener-ების თრექინგი და გასუფთავება
// =============================================================================
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

// =============================================================================
// Helper: ვაგონის ნომრის სიგრძე (8, 10, 12)
// =============================================================================
function getAllowedWagonLength(netContainer) {
    const input = netContainer.querySelector("#magonNumLeght_3");
    const value = input?.value?.trim();
    const num = parseInt(value, 10);
    return (Number.isInteger(num) && [8, 10, 12].includes(num)) ? num : 8;
}

// =============================================================================
// Helper: conId_3-ის მიღება
// =============================================================================
function getConId(netContainer) {
    const input = netContainer.querySelector("#conId_3");
    return input?.value?.trim() || "unknown";
}

// =============================================================================
// Helper: კავშირის ინდიკატორის განახლება (#con3-indicator)
// =============================================================================
function updateConIndicator(success) {
    const indicator = document.querySelector("#con3-indicator");
    if (!indicator) return;

    indicator.style.transition = "background-color 0.5s ease";
    indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";

    // ავტომატური გაქრობა 5 წამში (შეგიძლია ამოიღო თუ მუდმივი გინდა)
    const id = setTimeout(() => {
        indicator.style.backgroundColor = "";
    }, 5000);

    intervalIds.add(id);
}

// =============================================================================
// INITIAL LOAD: /showweighingWagons3 → #operation-data3-container
// =============================================================================
async function loadInitialWagonData(netContainer, updateIndicator) {
    if (!netContainer) return;

    const targetContainer = netContainer.querySelector("#operation-data3-container");
    if (!targetContainer) return;

    try {
        const response = await fetch("/showweighingWagons3", { method: "POST" });
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

// =============================================================================
// BIND EDIT FORMS: .oprdata3-set-from ფორმები + ავტომატური გაგზავნა
// =============================================================================
function bindEditWagonForm(netContainer, updateIndicator) {
    const editForms = netContainer.querySelectorAll('form.oprdata3-set-from');
    const allowedLength = getAllowedWagonLength(netContainer);

    editForms.forEach((editForm) => {
        const editBtn = editForm.querySelector('.oprdata3-set-btn');
        const wagonNumberInput = editForm.querySelector('.scale3-operdata-wagonNum-input');

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

// =============================================================================
// VIDEO: HLS.js ინიციალიზაცია
// =============================================================================
function initVideoScale3() {
    const video = document.getElementById('player-3');
    if (!video || !video.getAttribute('data-hls-src')) return;

    if (hlsInstanceScale3) {
        hlsInstanceScale3.destroy();
        hlsInstanceScale3 = null;
    }

    if (Hls.isSupported()) {
        hlsInstanceScale3 = new Hls({
            maxBufferLength: 15,
            maxMaxBufferLength: 20,
            maxBufferSize: 20 * 1000 * 1000,
            liveSyncDurationCount: 3,
            liveMaxLatencyDurationCount: 8,
            xhrSetup: (xhr) => { xhr.timeout = 10000; },
        });

        hlsInstanceScale3.loadSource(video.getAttribute('data-hls-src'));
        hlsInstanceScale3.attachMedia(video);

        hlsInstanceScale3.on(Hls.Events.MANIFEST_PARSED, () => video.play().catch(() => {}));
        hlsInstanceScale3.on(Hls.Events.ERROR, (event, data) => {
            if (data.fatal || data.details === 'levelLoadError') {
                hlsInstanceScale3.destroy();
                hlsInstanceScale3 = null;
                setTimeout(initVideoScale3, 2000);
            }
        });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = video.getAttribute('data-hls-src');
        video.play().catch(() => {});
    }
}

function observeVideoScale3(container) {
    if (videoObserver) videoObserver.disconnect();

    videoObserver = new MutationObserver(() => {
        const video = document.getElementById('player-3');
        if (video && !hlsInstanceScale3 && video.getAttribute('data-hls-src')) {
            initVideoScale3();
        }
    });

    videoObserver.observe(container, { childList: true, subtree: true });
}

// =============================================================================
// SSE: წონის + კავშირის სტატუსის მიღება
// =============================================================================
function connectWeightSSE(netContainer, updateIndicator) {
    if (weightEventSource) {
        weightEventSource.close();
        weightEventSource = null;
    }

    weightEventSource = new EventSource('/sendscale3');

    weightEventSource.addEventListener(getConId(netContainer), (e) => {
        const data = e.data.trim();

        switch (data) {
            case 'update-data-container':
                loadInitialWagonData(netContainer, updateIndicator);
                break;

            case 'update-data-works-start':
                document.getElementById('w-indic-3').value = "START";
                document.getElementById('w-indic-3').style.color = '#fdec04ff';
                break;

            case 'update-data-works-stop':
                document.getElementById('w-indic-3').value = "END";
                document.getElementById('w-indic-3').style.color = '#fd048dff';
                break;

            case 'update-con-indicator':
                updateConIndicator(true);
                break;

            default:
                // წონის მნიშვნელობა
                const weightInput = document.getElementById('w-indic-3');
                if (weightInput) {
                    weightInput.value = data;
                    weightInput.style.color = '#04cbfdff';
                    setTimeout(() => { weightInput.style.color = ''; }, 1000);
                }
                break;
        }
    });

    weightEventSource.onopen = () => {
        console.log('SSE connected: /sendscale3');
        updateConIndicator(true);
    };

    weightEventSource.onerror = () => {
        updateConIndicator(false);
        weightEventSource.close();
        weightEventSource = null;
        setTimeout(() => connectWeightSSE(netContainer, updateIndicator), 2000);
    };
}

// =============================================================================
// MAIN INITIALIZATION — მხოლოდ ერთხელ!
// =============================================================================
export function initScale3Module() {
    if (isScale3Initialized) {
        console.warn("scale3 უკვე ინიციალიზებულია. გამოტოვება.");
        return;
    }

    isScale3Initialized = true;
    const netContainer = document.querySelector("#scale3-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) {
        isScale3Initialized = false;
        return;
    }

    function updateIndicator(success) {
        const indicator = document.querySelector("#scale3-indicator");
        if (indicator) {
            indicator.style.transition = "background-color 0.5s ease";
            indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";
            const id = setTimeout(() => indicator.style.backgroundColor = "", 5000);
            intervalIds.add(id);
        }
    }

    // თავდაპირველი მდგომარეობა — კავშირი ჯერ არ არის
    updateConIndicator(false);

    // 1. Load initial data & SSE
    loadInitialWagonData(netContainer, updateIndicator);
    connectWeightSSE(netContainer, updateIndicator);

    // 2. Helper: Reload module safely
    const reloadModule = async (actionUrl) => {
        cleanupScale3Module();

        try {
            const response = await fetch(actionUrl, { method: "POST" });
            if (!response.ok) throw new Error(`Status: ${response.status}`);
            const html = await response.text();
            content.innerHTML = html;

            if (content.querySelector("#scale3-container")) {
                const mod = await import(`/scripts/scale3.js?v=${Date.now()}`);
                mod.initScale3Module();
            }
            updateIndicator(true);
        } catch (err) {
            content.innerHTML = `<p style="color:red;">Error: ${err.message}</p>`;
            updateIndicator(false);
        }
    };

    // 3. START WEIGHING
    const startForm = netContainer.querySelector('form[action="/startWeighing_3"]' );
    const startBtn = startForm?.querySelector("#scale3-start-weighing-btn");
    if (startForm && startBtn) {
        const newBtn = startBtn.cloneNode(true);
        startBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(startForm.action);
        });
    }

    // 4. DONE WEIGHING
    const doneForm = netContainer.querySelector('form[action="/doneWeighing_3"]');
    const doneBtn = doneForm?.querySelector("#scale3-done-weighing-btn");
    if (doneForm && doneBtn) {
        const newBtn = doneBtn.cloneNode(true);
        doneBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(doneForm.action);
        });
    }

    // 5. ABORT WEIGHING
    const abortForm = netContainer.querySelector('form[action="/abortWeighing_3"]');
    const abortBtn = abortForm?.querySelector("#scale3-abort-weighing-btn");
    if (abortForm && abortBtn) {
        const newBtn = abortBtn.cloneNode(true);
        abortBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(abortForm.action);
        });
    }

    // 6. ADD WAGON
    const addForm = netContainer.querySelector('form[action="/addwagonWeighing_3"]');
    const addBtn = addForm?.querySelector("#scale3-add-wagon-btn");
    const wagonNumberInput = addForm?.querySelector("#scale3-number-input");

    if (addForm && addBtn && wagonNumberInput) {
        const newBtn = addBtn.cloneNode(true);
        addBtn.replaceWith(newBtn);

        const allowedLength = getAllowedWagonLength(netContainer);
        let isSubmitting = false;

        const submitHandler = async (e) => {
            if (e) e.preventDefault();
            if (isSubmitting) return;
            isSubmitting = true;

            const targetContainer = netContainer.querySelector("#operation-data3-container");
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

                ['#scale3-number-input', '#scale3-product-input', '#scale3-count-input'].forEach(sel => {
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

    // 7. UPDATE ALL
    const updateForm = netContainer.querySelector('form[action="/updateAllWeighing_3"]');
    const updateBtn = updateForm?.querySelector("#scale3-update-weighing-btn");

    if (updateForm && updateBtn) {
        const newBtn = updateBtn.cloneNode(true);
        updateBtn.replaceWith(newBtn);

        trackEventListener(newBtn, "click", async (e) => {
            e.preventDefault();
            const targetContainer = netContainer.querySelector("#operation-data3-container");
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

    // 8. VIDEO
    initVideoScale3();
    observeVideoScale3(netContainer);

    // 9. Global cleanup access
    window.cleanupScale3Module = cleanupScale3Module;
}

// =============================================================================
// FULL CLEANUP
// =============================================================================
export function cleanupScale3Module() {
    console.log('Cleaning up scale3 module...');

    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    clearEventListeners();

    if (hlsInstanceScale3) {
        hlsInstanceScale3.destroy();
        hlsInstanceScale3 = null;
    }

    if (videoObserver) {
        videoObserver.disconnect();
        videoObserver = null;
    }

    if (weightEventSource) {
        weightEventSource.close();
        weightEventSource = null;
    }

    // ინდიკატორების გასუფთავება
    const indicators = ["#scale3-indicator", "#con3-indicator"];
    indicators.forEach(sel => {
        const el = document.querySelector(sel);
        if (el) el.style.backgroundColor = "";
    });

    isScale3Initialized = false;

    console.log('scale3 module fully cleaned');
}

// Auto-cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (typeof cleanupScale3Module === 'function') {
        cleanupScale3Module();
    }
});