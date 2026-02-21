// =============================================================================
// archive.js — მოდული #archive-container-ისთვის (განახლებული 2025/2026 სტილში)
// =============================================================================

let intervalIds = new Set();
let eventListeners = new Map();
let isArchiveInitialized = false;

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
// Helper: #train-data-container-ის განახლება + rebinding
// =============================================================================
function updateTrainDataContainer(container, html, updateIndicator) {
    const target = container.querySelector("#train-data-container");
    if (!target) return;

    target.innerHTML = html;
    bindArchiveTrainForms(container, updateIndicator);
    updateIndicator?.(true);
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

    // ფილტრის ფორმა
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

    // Clear ღილაკი (ახლა action="/archive/showTrains" → სიის გასუფთავება/ჩატვირთვა)
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

                    // ველების გასუფთავება
                    const dateFrom = container.querySelector('input[name="dateFrom"]');
                    const dateTo = container.querySelector('input[name="dateTo"]');
                    if (dateFrom) dateFrom.value = '';
                    if (dateTo) dateTo.value = '';
                } catch (err) {
                    updateTrainDataContainer(container, `<p style="color:red;">გასუფთავება ვერ მოხერხდა: ${err.message}</p>`, updateIndicator);
                    updateIndicator(false);
                }
            });
        }
    }

    // Fast buttons
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
        // 1. დიდი ღილაკი → edit/details
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

        // 2. Show Report → PDF
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

    // ბაინდინგი ყველაფრის
    setTimeout(() => {
        bindArchiveFiltersAndButtons(container, updateArchiveIndicator);
        bindArchiveTrainForms(container, updateArchiveIndicator);
        console.log("Filters + train forms bound");
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