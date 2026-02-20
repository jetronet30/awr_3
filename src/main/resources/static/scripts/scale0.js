// =============================================================================
// scale0.js — მოდული #scale0-container-ისთვის (განახლებული 2025/2026)
// =============================================================================

let intervalIds = new Set();
let eventListeners = new Map();
let hlsInstanceScale0 = null;
let videoObserver = null;
let weightEventSource = null;

// === ინიციალიზაციის ფლაგი (მხოლოდ ერთხელ) ===
let isScale0Initialized = false;

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
    const input = netContainer.querySelector("#magonNumLeght_0");
    const value = input?.value?.trim();
    const num = parseInt(value, 10);
    return (Number.isInteger(num) && [8, 10, 12].includes(num)) ? num : 8;
}

// =============================================================================
// Helper: conId_0-ის მიღება
// =============================================================================
function getConId(netContainer) {
    const input = netContainer.querySelector("#conId_0");
    return input?.value?.trim() || "unknown";
}

// =============================================================================
// Helper: კავშირის ინდიკატორის განახლება (#con0-indicator)
// =============================================================================
function updateConIndicator(success) {
    const indicator = document.querySelector("#con0-indicator");
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
// INITIAL LOAD: /showweighingWagons0 → #operation-data0-container
// =============================================================================
async function loadInitialWagonData(netContainer, updateIndicator) {
    if (!netContainer) return;

    const targetContainer = netContainer.querySelector("#operation-data0-container");
    if (!targetContainer) return;

    try {
        const response = await fetch("/showweighingWagons0", { method: "POST" });
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
// BIND EDIT FORMS: .oprdata0-set-from ფორმები + ავტომატური გაგზავნა
// =============================================================================
function bindEditWagonForm(netContainer, updateIndicator) {
    const editForms = netContainer.querySelectorAll('form.oprdata0-set-from');
    const allowedLength = getAllowedWagonLength(netContainer);

    editForms.forEach((editForm) => {
        const editBtn = editForm.querySelector('.oprdata0-set-btn');
        const wagonNumberInput = editForm.querySelector('.scale0-operdata-wagonNum-input');

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

// ================================================
// Print ღილაკი — იყენებს iframe-ს და ბრაუზერის print()
// ================================================
function initScale0PrintButton() {
    const printBtn = document.getElementById("scale0-print-btn");
    if (!printBtn) return;

    // წინა listener-ების გაწმენდა (თუ გვაქვს დუბლიკატი)
    const cleanBtn = printBtn.cloneNode(true);
    printBtn.replaceWith(cleanBtn);

    cleanBtn.addEventListener("click", () => {
        // ქმნით ფარულ iframe-ს
        const iframe = document.createElement("iframe");
        iframe.style.cssText = `
            position: fixed;
            right:   -9999px;
            bottom:  -9999px;
            width:    1px;
            height:   1px;
            border:   none;
            visibility: hidden;
        `;

        // დროებითი query string ქეშის თავიდან ასაცილებლად
        iframe.src = `/pdf0?t=${Date.now()}`;

        document.body.appendChild(iframe);

        iframe.onload = () => {
            try {
                // მცირე დაყოვნება PDF-ის ჩატვირთვისთვის (განსაკუთრებით Chrome/Edge-ში)
                setTimeout(() => {
                    const iframeWin = iframe.contentWindow || iframe.contentDocument.defaultView;
                    if (iframeWin) {
                        iframeWin.focus();
                        iframeWin.print();
                    }
                }, 1000);  // 600–1500 ms ჩვეულებრივ საკმარისია
            } catch (err) {
                console.error("Print error:", err);
                alert("ბეჭდვის ფანჯარა ვერ გაიხსნა.\nსცადეთ Ctrl + P ხელით.");
            }

            // iframe-ის ავტომატური წაშლა 15 წამში
            setTimeout(() => {
                if (iframe.parentNode) {
                    iframe.parentNode.removeChild(iframe);
                }
            }, 15000);
        };

        // თუ iframe არ ჩაიტვირთა 20 წამში
        iframe.onerror = () => {
            alert("PDF ფაილის ჩატვირთვა ვერ მოხერხდა (/pdf0).");
            if (iframe.parentNode) iframe.parentNode.removeChild(iframe);
        };
    });
}

// =============================================================================
// VIDEO: HLS.js ინიციალიზაცია
// =============================================================================
function initVideoScale0() {
    const video = document.getElementById('player-0');
    if (!video || !video.getAttribute('data-hls-src')) return;

    if (hlsInstanceScale0) {
        hlsInstanceScale0.destroy();
        hlsInstanceScale0 = null;
    }

    if (Hls.isSupported()) {
        hlsInstanceScale0 = new Hls({
            maxBufferLength: 15,
            maxMaxBufferLength: 20,
            maxBufferSize: 20 * 1000 * 1000,
            liveSyncDurationCount: 3,
            liveMaxLatencyDurationCount: 8,
            xhrSetup: (xhr) => { xhr.timeout = 10000; },
        });

        hlsInstanceScale0.loadSource(video.getAttribute('data-hls-src'));
        hlsInstanceScale0.attachMedia(video);

        hlsInstanceScale0.on(Hls.Events.MANIFEST_PARSED, () => video.play().catch(() => {}));
        hlsInstanceScale0.on(Hls.Events.ERROR, (event, data) => {
            if (data.fatal || data.details === 'levelLoadError') {
                hlsInstanceScale0.destroy();
                hlsInstanceScale0 = null;
                setTimeout(initVideoScale0, 2000);
            }
        });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = video.getAttribute('data-hls-src');
        video.play().catch(() => {});
    }
}

function observeVideoScale0(container) {
    if (videoObserver) videoObserver.disconnect();

    videoObserver = new MutationObserver(() => {
        const video = document.getElementById('player-0');
        if (video && !hlsInstanceScale0 && video.getAttribute('data-hls-src')) {
            initVideoScale0();
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

    weightEventSource = new EventSource('/sendscale0');

    weightEventSource.addEventListener(getConId(netContainer), (e) => {
        const data = e.data.trim();

        switch (data) {
            case 'update-data-container':
                loadInitialWagonData(netContainer, updateIndicator);
                break;

            case 'update-data-works-start':
                document.getElementById('w-indic-0').value = "START";
                document.getElementById('w-indic-0').style.color = '#fdec04ff';
                break;

            case 'update-data-works-stop':
                document.getElementById('w-indic-0').value = "END";
                document.getElementById('w-indic-0').style.color = '#fd048dff';
                break;

            case 'update-con-indicator':
                updateConIndicator(true);
                break;

            default:
                // წონის მნიშვნელობა
                const weightInput = document.getElementById('w-indic-0');
                if (weightInput) {
                    weightInput.value = data;
                    weightInput.style.color = '#04cbfdff';
                    setTimeout(() => { weightInput.style.color = ''; }, 1000);
                }
                break;
        }
    });

    weightEventSource.onopen = () => {
        console.log('SSE connected: /sendscale0');
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
export function initScale0Module() {
    if (isScale0Initialized) {
        console.warn("scale0 უკვე ინიციალიზებულია. გამოტოვება.");
        return;
    }

    isScale0Initialized = true;
    const netContainer = document.querySelector("#scale0-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) {
        isScale0Initialized = false;
        return;
    }

    function updateIndicator(success) {
        const indicator = document.querySelector("#scale0-indicator");
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

            if (content.querySelector("#scale0-container")) {
                const mod = await import(`/scripts/scale0.js?v=${Date.now()}`);
                mod.initScale0Module();
            }
            updateIndicator(true);
        } catch (err) {
            content.innerHTML = `<p style="color:red;">Error: ${err.message}</p>`;
            updateIndicator(false);
        }
    };

    // 3. START WEIGHING
    const startForm = netContainer.querySelector('form[action="/startWeighing_0"]' );
    const startBtn = startForm?.querySelector("#scale0-start-weighing-btn");
    if (startForm && startBtn) {
        const newBtn = startBtn.cloneNode(true);
        startBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(startForm.action);
        });
    }

    // 4. DONE WEIGHING
    const doneForm = netContainer.querySelector('form[action="/doneWeighing_0"]');
    const doneBtn = doneForm?.querySelector("#scale0-done-weighing-btn");
    if (doneForm && doneBtn) {
        const newBtn = doneBtn.cloneNode(true);
        doneBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(doneForm.action);
        });
    }

    // 5. ABORT WEIGHING
    const abortForm = netContainer.querySelector('form[action="/abortWeighing_0"]');
    const abortBtn = abortForm?.querySelector("#scale0-abort-weighing-btn");
    if (abortForm && abortBtn) {
        const newBtn = abortBtn.cloneNode(true);
        abortBtn.replaceWith(newBtn);
        trackEventListener(newBtn, "click", (e) => {
            e.preventDefault();
            reloadModule(abortForm.action);
        });
    }

    // 6. ADD WAGON
    const addForm = netContainer.querySelector('form[action="/addwagonWeighing_0"]');
    const addBtn = addForm?.querySelector("#scale0-add-wagon-btn");
    const wagonNumberInput = addForm?.querySelector("#scale0-number-input");

    if (addForm && addBtn && wagonNumberInput) {
        const newBtn = addBtn.cloneNode(true);
        addBtn.replaceWith(newBtn);

        const allowedLength = getAllowedWagonLength(netContainer);
        let isSubmitting = false;

        const submitHandler = async (e) => {
            if (e) e.preventDefault();
            if (isSubmitting) return;
            isSubmitting = true;

            const targetContainer = netContainer.querySelector("#operation-data0-container");
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

                ['#scale0-number-input', '#scale0-product-input', '#scale0-count-input'].forEach(sel => {
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
    const updateForm = netContainer.querySelector('form[action="/updateAllWeighing_0"]');
    const updateBtn = updateForm?.querySelector("#scale0-update-weighing-btn");

    if (updateForm && updateBtn) {
        const newBtn = updateBtn.cloneNode(true);
        updateBtn.replaceWith(newBtn);

        trackEventListener(newBtn, "click", async (e) => {
            e.preventDefault();
            const targetContainer = netContainer.querySelector("#operation-data0-container");
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
    initVideoScale0();
    observeVideoScale0(netContainer);
    initScale0PrintButton();

    // 9. Global cleanup access
    window.cleanupScale0Module = cleanupScale0Module;
}

// =============================================================================
// FULL CLEANUP
// =============================================================================
export function cleanupScale0Module() {
    console.log('Cleaning up scale0 module...');

    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    clearEventListeners();

    if (hlsInstanceScale0) {
        hlsInstanceScale0.destroy();
        hlsInstanceScale0 = null;
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
    const indicators = ["#scale0-indicator", "#con0-indicator"];
    indicators.forEach(sel => {
        const el = document.querySelector(sel);
        if (el) el.style.backgroundColor = "";
    });

    isScale0Initialized = false;

    console.log('scale0 module fully cleaned');
}

// Auto-cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (typeof cleanupScale0Module === 'function') {
        cleanupScale0Module();
    }
});