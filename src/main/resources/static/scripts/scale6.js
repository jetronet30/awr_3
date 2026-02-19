// =============================================================================
// scale6.js — მოდული #scale6-container-ისთვის (განახლებული 2025/2026)
// =============================================================================

let intervalIds = new Set();
let eventListeners = new Map();
let hlsInstanceScale6 = null;
let videoObserver = null;
let weightEventSource = null;

// === ინიციალიზაციის ფლაგი (მხოლოდ ერთხელ) ===
let isScale6Initialized = false;

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
    const input = netContainer.querySelector("#magonNumLeght_6");
    const value = input?.value?.trim();
    const num = parseInt(value, 10);
    return (Number.isInteger(num) && [8, 10, 12].includes(num)) ? num : 8;
}

// =============================================================================
// Helper: conId_6-ის მიღება
// =============================================================================
function getConId(netContainer) {
    const input = netContainer.querySelector("#conId_6");
    return input?.value?.trim() || "unknown";
}

// =============================================================================
// Helper: კავშირის ინდიკატორის განახლება (#con6-indicator)
// =============================================================================
function updateConIndicator(success) {
    const indicator = document.querySelector("#con6-indicator");
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
// INITIAL LOAD: /showweighingWagons6 → #operation-data6-container
// =============================================================================
async function loadInitialWagonData(netContainer, updateIndicator) {
    if (!netContainer) return;

    const targetContainer = netContainer.querySelector("#operation-data6-container");
    if (!targetContainer) return;

    try {
        const response = await fetch("/showweighingWagons6", { method: "POST" });
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
// BIND EDIT FORMS: .oprdata6-set-from ფორმები + ავტომატური გაგზავნა
// =============================================================================
function bindEditWagonForm(netContainer, updateIndicator) {
    const editForms = netContainer.querySelectorAll('form.oprdata6-set-from');
    const allowedLength = getAllowedWagonLength(netContainer);

    editForms.forEach((editForm) => {
        const editBtn = editForm.querySelector('.oprdata6-set-btn');
        const wagonNumberInput = editForm.querySelector('.scale6-operdata-wagonNum-input');

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
function initVideoScale6() {
    const video = document.getElementById('player-6');
    if (!video || !video.getAttribute('data-hls-src')) return;

    if (hlsInstanceScale6) {
        hlsInstanceScale6.destroy();
        hlsInstanceScale6 = null;
    }

    if (Hls.isSupported()) {
        hlsInstanceScale6 = new Hls({
            maxBufferLength: 15,
            maxMaxBufferLength: 20,
            maxBufferSize: 20 * 1000 * 1000,
            liveSyncDurationCount: 3,
            liveMaxLatencyDurationCount: 8,
            xhrSetup: (xhr) => { xhr.timeout = 10000; },
        });

        hlsInstanceScale6.loadSource(video.getAttribute('data-hls-src'));
        hlsInstanceScale6.attachMedia(video);

        hlsInstanceScale6.on(Hls.Events.MANIFEST_PARSED, () => video.play().catch(() => {}));
        hlsInstanceScale6.on(Hls.Events.ERROR, (event, data) => {
            if (data.fatal || data.details === 'levelLoadError') {
                hlsInstanceScale6.destroy();
                hlsInstanceScale6 = null;
                setTimeout(initVideoScale6, 2000);
            }
        });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = video.getAttribute('data-hls-src');
        video.play().catch(() => {});
    }
}

function observeVideoScale6(container) {
    if (videoObserver) videoObserver.disconnect();

    videoObserver = new MutationObserver(() => {
        const video = document.getElementById('player-6');
        if (video && !hlsInstanceScale6 && video.getAttribute('data-hls-src')) {
            initVideoScale6();
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

    weightEventSource = new EventSource('/sendscale6');

    weightEventSource.addEventListener(getConId(netContainer), (e) => {
        const data = e.data.trim();

        switch (data) {
            case 'update-data-container':
                loadInitialWagonData(netContainer, updateIndicator);
                break;

            case 'update-data-works-start':
                document.getElementById('w-indic-6').value = "START";
                document.getElementById('w-indic-6').style.color = '#fdec04ff';
                break;

            case 'update-data-works-stop':
                document.getElementById('w-indic-6').value = "END";
                document.getElementById('w-indic-6').style.color = '#fd048dff';
                break;

            case 'update-con-indicator':
                updateConIndicator(true);
                break;

            default:
                // წონის მნიშვნელობა
                const weightInput = document.getElementById('w-indic-6');
                if (weightInput) {
                    weightInput.value = data;
                    weightInput.style.color = '#04cbfdff';
                    setTimeout(() => { weightInput.style.color = ''; }, 1000);
                }
                break;
        }
    });

    weightEventSource.onopen = () => {
        console.log('SSE connected: /sendscale6');
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
export function initScale6Module() {
    if (isScale6Initialized) {
        console.warn("scale6 უკვე ინიციალიზებულია. გამოტოვება.");
        return;
    }

    isScale6Initialized = true;
    const netContainer = document.querySelector("#scale6-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) {
        isScale6Initialized = false;
        return;
    }

    function updateIndicator(success) {
        const indicator = document.querySelector("#scale6-indicator");
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
        cleanupScale0Module();

        try {
            const response = await fetch(actionUrl, { method: "POST" });
            if (!response.ok) throw new Error(`Status: ${response.status}`);
            const html = await response.text();
            content.innerHTML = html;

            if (content.querySelector("#scale6-container")) {
                const mod = await import(`/scripts/scale6.js?v=${Date.now()}`);
                mod.initScale6Module();
            }
            updateIndicator(true);
        } catch (err) {
            content.innerHTML = `<p style="color:red;">Error: ${err.message}</p>`;
            updateIndicator(false);
        }
    };

    // 3. START WEIGHING
    const startForm = netContainer.querySelector('form[action="/startWeighing_6"]' );
    const startBtn = startForm?.querySelector("#scale6-start-weighing-btn");
    if (startForm && startBtn) {
        const newBtn = startBtn.cloneNode(true);
        startBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(startForm.action);
        });
    }

    // 4. DONE WEIGHING
    const doneForm = netContainer.querySelector('form[action="/doneWeighing_6"]');
    const doneBtn = doneForm?.querySelector("#scale6-done-weighing-btn");
    if (doneForm && doneBtn) {
        const newBtn = doneBtn.cloneNode(true);
        doneBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(doneForm.action);
        });
    }

    // 5. ABORT WEIGHING
    const abortForm = netContainer.querySelector('form[action="/abortWeighing_6"]');
    const abortBtn = abortForm?.querySelector("#scale6-abort-weighing-btn");
    if (abortForm && abortBtn) {
        const newBtn = abortBtn.cloneNode(true);
        abortBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(abortForm.action);
        });
    }

    // 6. ADD WAGON
    const addForm = netContainer.querySelector('form[action="/addwagonWeighing_6"]');
    const addBtn = addForm?.querySelector("#scale6-add-wagon-btn");
    const wagonNumberInput = addForm?.querySelector("#scale6-number-input");

    if (addForm && addBtn && wagonNumberInput) {
        const newBtn = addBtn.cloneNode(true);
        addBtn.replaceWith(newBtn);

        const allowedLength = getAllowedWagonLength(netContainer);
        let isSubmitting = false;

        const submitHandler = async (e) => {
            if (e) e.preventDefault();
            if (isSubmitting) return;
            isSubmitting = true;

            const targetContainer = netContainer.querySelector("#operation-data6-container");
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

                ['#scale6-number-input', '#scale6-product-input', '#scale6-count-input'].forEach(sel => {
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
    const updateForm = netContainer.querySelector('form[action="/updateAllWeighing_6"]');
    const updateBtn = updateForm?.querySelector("#scale6-update-weighing-btn");

    if (updateForm && updateBtn) {
        const newBtn = updateBtn.cloneNode(true);
        updateBtn.replaceWith(newBtn);

        trackEventListener(newBtn, "click", async (e) => {
            e.preventDefault();
            const targetContainer = netContainer.querySelector("#operation-data6-container");
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
    initVideoScale6();
    observeVideoScale6(netContainer);

    // 9. Global cleanup access
    window.cleanupScale6Module = cleanupScale6Module;
}

// =============================================================================
// FULL CLEANUP
// =============================================================================
export function cleanupScale6Module() {
    console.log('Cleaning up scale6 module...');

    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    clearEventListeners();

    if (hlsInstanceScale6) {
        hlsInstanceScale6.destroy();
        hlsInstanceScale6 = null;
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
    const indicators = ["#scale6-indicator", "#con6-indicator"];
    indicators.forEach(sel => {
        const el = document.querySelector(sel);
        if (el) el.style.backgroundColor = "";
    });

    isScale6Initialized = false;

    console.log('scale6 module fully cleaned');
}

// Auto-cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (typeof cleanupScale6Module === 'function') {
        cleanupScale6Module();
    }
});