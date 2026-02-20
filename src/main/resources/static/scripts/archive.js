// =============================================================================
// archive.js — მოდული #archive-container-ისთვის
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
// INITIAL LOAD: /showTrains → #train-data-container
// =============================================================================
async function loadInitialTrainData(container, updateIndicator = null) {
    if (!container) return;

    const target = container.querySelector("#train-data-container");
    if (!target) return;

    try {
        const response = await fetch("/showTrains", { method: "POST" });
        if (!response.ok) throw new Error(`Initial load failed: ${response.status}`);

        const html = await response.text();
        target.innerHTML = html;

        updateIndicator?.(true);
        // თუ მოგვიანებით დაგჭირდება ღილაკების/ფორმების ბაინდინგი აქ დაწერე
        // მაგ: bindArchiveForms(container, updateIndicator);
    } catch (err) {
        target.innerHTML = `<p style="color:red;">ჩატვირთვის შეცდომა: ${err.message}</p>`;
        updateIndicator?.(false);
    }
}

// =============================================================================
// ინდიკატორის განახლება (თუ გინდა დაამატო #archive-indicator)
// =============================================================================
function updateIndicator(success) {
    const indicator = document.querySelector("#archive-indicator"); // თუ დაამატებ HTML-ში
    if (!indicator) return;

    indicator.style.transition = "background-color 0.5s ease";
    indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";
    const id = setTimeout(() => indicator.style.backgroundColor = "", 5000);
    intervalIds.add(id);
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
    loadInitialTrainData(container, updateIndicator);

    // თუ მოგვიანებით დაგჭირდება განახლების ღილაკი ან SSE
    // მაგალითად:
    // const refreshBtn = container.querySelector('#archive-refresh-btn');
    // if (refreshBtn) {
    //     trackEventListener(refreshBtn, 'click', () => loadInitialTrainData(container, updateIndicator));
    // }

    // თუ გინდა SSE ან polling დამატება — აქ ჩაწერე

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

    isArchiveInitialized = false;

    console.log('archive module fully cleaned');
}

// Auto-cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (typeof cleanupArchiveModule === 'function') {
        cleanupArchiveModule();
    }
});

// თუ გვერდი პირდაპირ იტვირთება — გაუშვი ავტომატურად
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initArchiveModule);
} else {
    initArchiveModule();
}