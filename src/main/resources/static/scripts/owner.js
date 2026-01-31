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

export function initOwnerModule() {
    const netContainer = document.querySelector("#owner-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) return;

    function updateIndicator(success) {
        const indicator = document.querySelector("#owner-indicator");
        if (indicator) {
            indicator.style.transition = "background-color 0.5s ease";
            indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";
            const timeoutId = setTimeout(() => {
                indicator.style.backgroundColor = "";
            }, 5000);
            intervalIds.add(timeoutId);
        }
    }

    /** --- ⚙️ data set --- */
    const form = netContainer.querySelector("#owner-set-from");
    if (!form) return;

    // საერთო event handler – ორივე ღილაკისთვის
    const handleSubmit = async (e) => {
        e.preventDefault();
        const formData = new FormData(form);
        try {
            const response = await fetch(form.action, { method: "POST", body: formData });
            if (!response.ok) throw new Error(`Server error: ${response.status}`);
            const result = await response.json();
            updateIndicator(result.success);
            if (!result.success && result.message) {
                alert("OWNER პარამეტრების განახლება ჩაიშალა: " + result.message);
            } else if (!result.success) {
                alert("OWNER პარამეტრების განახლება ჩაიშალა: უცნობი შეცდომა");
            }
        } catch (err) {
            content.innerHTML = `<p style="color:red;">Error applying OWNER settings.</p>`;
            updateIndicator(false);
            alert("შეცდომა OWNER პარამეტრების განახლებისას: " + err.message);
        }
    };

    // ორივე ღილაკს ვუკავშირებთ ერთსა და იმავე handler-ს
    const setBtn = form.querySelector("#owner-set-btn");
    const cloneBtn = netContainer.querySelector("#owner-set-btn-clone");

    if (setBtn) trackEventListener(setBtn, "click", handleSubmit);
    if (cloneBtn) trackEventListener(cloneBtn, "click", handleSubmit);


    /** --- 🌐 write reboot ფორმა --- */
    const setWRForm = netContainer.querySelector('form[action="/owner-save-and-reboot"]');
    const setWRBtn = setWRForm?.querySelector("#owner-set-and-reboot-btn");

    if (setWRForm && setWRBtn) {
        setWRBtn.replaceWith(setWRBtn.cloneNode(true));
        const newSetWRBtn = netContainer.querySelector("#owner-set-and-reboot-btn");

        const handler = async (e) => {
            e.preventDefault();
            const formData = new FormData(setWRForm);
            try {
                const response = await fetch(setWRForm.action, { method: "POST", body: formData });
                if (!response.ok) throw new Error(`Server error: ${response.status}`);
                const html = await response.text();
                content.innerHTML = html;
                if (content.querySelector("#owner-container")) {
                    const mod = await import(`/scripts/owner.js?v=${Date.now()}`);
                    mod.initOwnerModule();
                }
                updateIndicator(true);
            } catch (err) {
                content.innerHTML = `<p style="color:red;">Error applying OWNER settings.</p>`;
                updateIndicator(false);
                alert("გადატვირთვის  შეცდომა: " + err.message);
            }
        };
        trackEventListener(newSetWRBtn, "click", handler);
    }
}

export function cleanupOwnerModule() {
    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();
    clearEventListeners();
    console.log('✅ owner module cleaned up');
}
