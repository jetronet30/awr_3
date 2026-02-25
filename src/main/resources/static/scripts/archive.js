// =============================================================================
// archive.js — მოდული #archive-container-ისთვის (განახლებული 2025/2026 სტილში)
// =============================================================================

let intervalIds = new Set();
let eventListeners = new Map();
let isArchiveInitialized = false;
let printObserver = null;
let videoObserver = null;

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
// Helper: ინდიკატორის განახლება (#archive-indicator)
// =============================================================================
function updateArchiveIndicator(success) {
    const indicator = document.querySelector("#archive-indicator");
    if (!indicator) return;

    indicator.style.transition = "background-color 0.5s ease";
    indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";

    const id = setTimeout(() => {
        indicator.style.backgroundColor = "";
    }, 5000);

    intervalIds.add(id);
}

// =============================================================================
// Helper: ვაგონის ნომრის სიგრძე (8, 10, 12)
// =============================================================================
function getAllowedWagonLength(netContainer) {
    const input = netContainer.querySelector("#magonNumLeght_train");
    const value = input?.value?.trim();
    const num = parseInt(value, 10);
    return (Number.isInteger(num) && [8, 10, 12].includes(num)) ? num : 8;
}

// =============================================================================
// Helper: #train-data-container-ის განახლება + rebinding
// =============================================================================
function updateTrainDataContainer(container, html, updateIndicator) {
    const target = container.querySelector("#train-data-container");
    if (!target) return;

    target.innerHTML = html;

    bindArchiveTrainForms(container, updateIndicator);
    bindEditWagonForm(container, updateIndicator);
    observePrintButton();
    observeVideoArchive();  // ← ვიდეო კონტროლი

    updateIndicator?.(true);
}

// =============================================================================
// Observer: Print ღილაკის აღმოჩენა
// =============================================================================
function observePrintButton() {
    if (printObserver) printObserver.disconnect();

    printObserver = new MutationObserver(() => {
        const printBtn = document.getElementById("train-print-btn");
        if (printBtn && !printBtn.dataset.listenerAdded) {
            console.log("Print ღილაკი აღმოჩენილია! ID:", printBtn.dataset.trainId || "არ არის");

            printBtn.dataset.listenerAdded = "true";

            printBtn.addEventListener("click", () => {
                const trainId = printBtn.dataset.trainId;

                if (!trainId) {
                    alert("ვერ მოიძებნა Train ID");
                    return;
                }

                console.log(`Print requested for train ID: ${trainId}`);

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

                iframe.src = `/archive/showPDF/${trainId}?t=${Date.now()}`;

                document.body.appendChild(iframe);

                iframe.onload = () => {
                    console.log("PDF iframe ჩაიტვირთა");
                    setTimeout(() => {
                        const win = iframe.contentWindow || iframe.contentDocument?.defaultView;
                        if (win) {
                            win.focus();
                            win.print();
                            console.log("print() გამოძახებულია");
                        }
                    }, 1500);

                    setTimeout(() => {
                        if (iframe.parentNode) iframe.parentNode.removeChild(iframe);
                    }, 20000);
                };

                iframe.onerror = () => {
                    console.error("iframe load error");
                    alert("PDF ვერ ჩაიტვირთა");
                    if (iframe.parentNode) iframe.parentNode.removeChild(iframe);
                };
            });
        }
    });

    const target = document.querySelector("#train-data-container") || document.body;
    printObserver.observe(target, { childList: true, subtree: true });
}

// =============================================================================
// Observer + Logic: ორი ვიდეოს სინქრონიზაცია (შენი მუშა ლოგიკა + დინამიური)
// =============================================================================
function observeVideoArchive() {
    if (videoObserver) videoObserver.disconnect();

    videoObserver = new MutationObserver(() => {
        initSyncedVideoControls();
    });

    const target = document.querySelector("#train-data-container") || document.body;
    videoObserver.observe(target, { childList: true, subtree: true });

    // დამატებითი ცდა (თუ Observer გამოტოვებს)
    setTimeout(initSyncedVideoControls, 500);
}

function initSyncedVideoControls() {
    const vid1 = document.getElementById('player_1');
    const vid2 = document.getElementById('player_2');
    const playPauseBtn = document.getElementById('tarin-video-playPause');
    const progress = document.getElementById('train-video-progress');
    const timeDisplay = document.getElementById('time');

    if (!playPauseBtn || !progress || !timeDisplay) {
        return; // კონტროლი ჯერ არ ჩატვირთულა
    }

    // თუ უკვე ინიციალიზებულია — არ გავიმეოროთ
    if (playPauseBtn.dataset.initialized === "true") return;
    playPauseBtn.dataset.initialized = "true";

    let isPlaying = false;
    let duration = 0;

    // ვიდეოების სინქრონიზაცია (მთავარი vid1-ის მიხედვით)
    function syncVideos() {
        if (vid2 && Math.abs(vid1.currentTime - vid2.currentTime) > 0.1) {
            vid2.currentTime = vid1.currentTime;
        }
    }

    // მოვლენები vid1-ზე (მასტერი)
    vid1.addEventListener('timeupdate', () => {
        if (!duration && vid1.duration) {
            duration = vid1.duration;
            progress.max = duration;
        }
        progress.value = vid1.currentTime;
        const current = formatTime(vid1.currentTime);
        const total = formatTime(duration);
        timeDisplay.textContent = `${current} / ${total}`;

        syncVideos();
    });

    vid1.addEventListener('seeking', syncVideos);
    vid1.addEventListener('play', () => { 
        if (vid2) vid2.play().catch(() => {});
        isPlaying = true; 
        playPauseBtn.textContent = '⏸ Pause'; 
    });
    vid1.addEventListener('pause', () => { 
        if (vid2) vid2.pause();
        isPlaying = false; 
        playPauseBtn.textContent = '▶ Play'; 
    });

    // პროგრესის სლაიდერი (seek)
    progress.addEventListener('input', () => {
        vid1.currentTime = progress.value;
        if (vid2) vid2.currentTime = progress.value;
    });

    // Play/Pause ღილაკი
    playPauseBtn.addEventListener('click', () => {
        if (isPlaying) {
            vid1.pause();
        } else {
            vid1.play().catch(e => console.log("Autoplay blocked", e));
        }
    });

    // დროის ფორმატი
    function formatTime(seconds) {
        const min = Math.floor(seconds / 60);
        const sec = Math.floor(seconds % 60);
        return `${min}:${sec.toString().padStart(2, '0')}`;
    }

    // თუ vid1 დასრულდა
    vid1.addEventListener('ended', () => {
        vid1.currentTime = 0;
        if (vid2) vid2.currentTime = 0;
        vid1.play();
        if (vid2) vid2.play();
    });

    console.log("ვიდეო კონტროლი სრულად ინიციალიზებულია");
}

// =============================================================================
// INITIAL LOAD: /archive/showTrains → #train-data-container
// =============================================================================
async function loadInitialTrainData(container, updateIndicator = null) {
    if (!container) return;

    const target = container.querySelector("#train-data-container");
    if (!target) return;

    try {
        const response = await fetch("/archive/showTrains", { method: "POST" });
        if (!response.ok) throw new Error(`Initial load failed: ${response.status}`);

        const html = await response.text();
        updateTrainDataContainer(container, html, updateIndicator);
        console.log("Initial train data loaded and bound");
    } catch (err) {
        target.innerHTML = `<p style="color:red;">ჩატვირთვის შეცდომა: ${err.message}</p>`;
        updateIndicator?.(false);
        console.error("Initial load error:", err);
    }
}

// =============================================================================
// FILTER & ACTION FORMS BINDING
// =============================================================================
function bindArchiveFiltersAndButtons(container, updateIndicator) {
    if (!container) return;

    const filterForm = container.querySelector('form.archive-filter-form');
    if (filterForm) {
        const submitBtn = filterForm.querySelector('#archive-filter-btn');
        if (submitBtn) {
            const cleanBtn = submitBtn.cloneNode(true);
            submitBtn.replaceWith(cleanBtn);

            trackEventListener(cleanBtn, 'click', async (e) => {
                e.preventDefault();
                const formData = new FormData(filterForm);

                try {
                    const response = await fetch(filterForm.action, {
                        method: "POST",
                        body: formData
                    });
                    if (!response.ok) throw new Error(`Filter failed: ${response.status}`);

                    const html = await response.text();
                    updateTrainDataContainer(container, html, updateIndicator);
                } catch (err) {
                    updateTrainDataContainer(container, `<p style="color:red;">ფილტრაციის შეცდომა: ${err.message}</p>`, updateIndicator);
                    updateIndicator(false);
                }
            });
        }
    }

    const clearForm = container.querySelector('form.archive-clear-form');
    if (clearForm) {
        const clearBtn = clearForm.querySelector('#archive-clear-btn');
        if (clearBtn) {
            const cleanClearBtn = clearBtn.cloneNode(true);
            clearBtn.replaceWith(cleanClearBtn);

            trackEventListener(cleanClearBtn, 'click', async (e) => {
                e.preventDefault();

                try {
                    const response = await fetch(clearForm.action, { method: "POST" });
                    if (!response.ok) throw new Error(`Clear failed: ${response.status}`);

                    const html = await response.text();
                    updateTrainDataContainer(container, html, updateIndicator);

                    const dateFrom = container.querySelector('input[name="dateFrom"]');
                    const dateTo   = container.querySelector('input[name="dateTo"]');
                    if (dateFrom) dateFrom.value = '';
                    if (dateTo)   dateTo.value   = '';
                } catch (err) {
                    updateTrainDataContainer(container, `<p style="color:red;">გასუფთავება ვერ მოხერხდა: ${err.message}</p>`, updateIndicator);
                    updateIndicator(false);
                }
            });
        }
    }

    const fastButtons = container.querySelectorAll('#archive-fast-buttons-container form, #archive-container form[action="/archive/showTrains"]');
    fastButtons.forEach(form => {
        const btn = form.querySelector('input[type="button"]');
        if (!btn) return;

        const cleanBtn = btn.cloneNode(true);
        btn.replaceWith(cleanBtn);

        trackEventListener(cleanBtn, 'click', async (e) => {
            e.preventDefault();

            try {
                const response = await fetch(form.action, { method: "POST" });
                if (!response.ok) throw new Error(`Action failed: ${response.status}`);

                const html = await response.text();
                updateTrainDataContainer(container, html, updateIndicator);
            } catch (err) {
                updateTrainDataContainer(container, `<p style="color:red;">შეცდომა: ${err.message}</p>`, updateIndicator);
                updateIndicator(false);
            }
        });
    });
}

// =============================================================================
// Train forms binding (edit & show PDF)
// =============================================================================
function bindArchiveTrainForms(container, updateIndicator) {
    if (!container) return;

    const forms = container.querySelectorAll('#train-data-container .archiv-train-from');

    forms.forEach(form => {
        const mainBtn = form.querySelector('.archiv-train-btn');
        if (mainBtn) {
            const cleanBtn = mainBtn.cloneNode(true);
            mainBtn.replaceWith(cleanBtn);

            trackEventListener(cleanBtn, 'click', async (e) => {
                e.preventDefault();
                e.stopPropagation();

                console.log("დიდი ღილაკი დაჭერილია → ", form.action);

                const target = container.querySelector('#train-data-container');
                if (!target) return;

                try {
                    target.innerHTML = '<div style="padding:3rem;text-align:center;color:#666;">იტვირთება...</div>';

                    const response = await fetch(form.action, {
                        method: 'POST',
                        body: new FormData(form)
                    });

                    if (!response.ok) throw new Error(`HTTP ${response.status}`);

                    const html = await response.text();
                    target.innerHTML = html;

                    updateIndicator?.(true);
                    bindArchiveTrainForms(container, updateIndicator);
                    bindEditWagonForm(container, updateIndicator);

                } catch (err) {
                    target.innerHTML = `
                        <div style="padding:3rem;color:#c00;text-align:center;">
                            <p>შეცდომა: ${err.message}</p>
                            <button onclick="location.reload()">განახლება</button>
                        </div>`;
                    updateIndicator?.(false);
                }
            });
        }

        const reportBtn = form.querySelector('input[type="button"][value="Show Report"]');
        if (reportBtn) {
            const cleanReportBtn = reportBtn.cloneNode(true);
            reportBtn.replaceWith(cleanReportBtn);

            trackEventListener(cleanReportBtn, 'click', async (e) => {
                e.preventDefault();
                e.stopPropagation();

                console.log("Show Report დაჭერილია → ", reportBtn.formAction || form.action);

                const target = container.querySelector('#train-data-container');
                if (!target) return;

                try {
                    target.innerHTML = '<div style="padding:3rem;text-align:center;color:#666;">PDF იტვირთება...</div>';

                    let pdfUrl = reportBtn.formAction;
                    if (!pdfUrl) {
                        pdfUrl = form.action.replace('/edit/', '/showPDF/');
                    }

                    const response = await fetch(pdfUrl, {
                        method: 'POST',
                        body: new FormData(form)
                    });

                    if (!response.ok) throw new Error(`PDF HTTP ${response.status}`);

                    const blob = await response.blob();
                    const url = URL.createObjectURL(blob);

                    target.innerHTML = `
                        <iframe 
                            src="${url}" 
                            style="width:100%; height:85vh; border:none; display:block;"
                            title="Train Report PDF">
                        </iframe>
                    `;

                    updateIndicator?.(true);

                } catch (err) {
                    target.innerHTML = `
                        <div style="padding:3rem;color:#c00;text-align:center;">
                            <p>PDF ჩატვირთვა ვერ მოხერხდა: ${err.message}</p>
                            <button onclick="location.reload()">განახლება</button>
                        </div>`;
                    updateIndicator?.(false);
                }
            });
        }
    });
}

// =============================================================================
// BIND EDIT FORMS: .train-wagons-set-from ფორმები + ავტომატური გაგზავნა
// =============================================================================
function bindEditWagonForm(container, updateIndicator) {
    const editForms = container.querySelectorAll('form.train-wagons-set-from');
    const allowedLength = getAllowedWagonLength(container);

    editForms.forEach((editForm) => {
        const editBtn = editForm.querySelector('.train-wagons-set-btn');
        const wagonNumberInput = editForm.querySelector('.train-wagons-wagonNum-input');

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
// MAIN INITIALIZATION — მხოლოდ ერთხელ!
// =============================================================================
export function initArchiveModule() {
    if (isArchiveInitialized) {
        console.warn("archive უკვე ინიციალიზებულია. გამოტოვება.");
        return;
    }

    isArchiveInitialized = true;
    const container = document.querySelector("#archive-container");

    if (!container) {
        isArchiveInitialized = false;
        console.warn("#archive-container არ მოიძებნა");
        return;
    }

    // თავდაპირველი ჩატვირთვა
    loadInitialTrainData(container, updateArchiveIndicator);

    // Print და ვიდეოების დაკვირვება
    observePrintButton();
    observeVideoArchive();

    // ბაინდინგი
    setTimeout(() => {
        bindArchiveFiltersAndButtons(container, updateArchiveIndicator);
        bindArchiveTrainForms(container, updateArchiveIndicator);
        bindEditWagonForm(container, updateArchiveIndicator);
        console.log("All bindings initialized");
    }, 500);

    console.log("archive module initialized");
}

// =============================================================================
// FULL CLEANUP
// =============================================================================
export function cleanupArchiveModule() {
    console.log('Cleaning up archive module...');

    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    clearEventListeners();

    if (printObserver) printObserver.disconnect();
    if (videoObserver) videoObserver.disconnect();

    const indicator = document.querySelector("#archive-indicator");
    if (indicator) indicator.style.backgroundColor = "";

    isArchiveInitialized = false;

    console.log('archive module fully cleaned');
}

// Auto-cleanup
window.addEventListener('beforeunload', cleanupArchiveModule);

// ავტომატური გაშვება
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initArchiveModule);
} else {
    initArchiveModule();
}