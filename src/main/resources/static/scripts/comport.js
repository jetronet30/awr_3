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

export function initComportModule() {
    const netContainer = document.querySelector("#comPort-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) {
        return;
    }

    function updateIndicator(success) {
        const indicator = document.querySelector("#comPort-indicator");
        if (indicator) {
            indicator.style.transition = "background-color 0.5s ease";
            indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";
            const timeoutId = setTimeout(() => {
                indicator.style.backgroundColor = "";
            }, 5000);
            intervalIds.add(timeoutId); // Track timeout
        }
    }

    /** --- ⚙️ setComPortSettings ფორმები --- */
    const lanSettingForms = netContainer.querySelectorAll(".comPort-set-from");
    lanSettingForms.forEach(form => {
        const setBtn = form.querySelector(".comPort-set-btn");
        if (setBtn) {
            setBtn.replaceWith(setBtn.cloneNode(true));
            const newSetBtn = form.querySelector(".comPort-set-btn");

            const handler = async (e) => {
                e.preventDefault();
                const formData = new FormData(form);
                try {
                    const response = await fetch(form.action, { method: "POST", body: formData });
                    if (!response.ok) throw new Error(`Server error: ${response.status}`);
                    const result = await response.json();
                    updateIndicator(result.success);
                    if (!result.success && result.message) {
                        alert("COMPORT პარამეტრების განახლება ჩაიშალა: " + result.message);
                    } else if (!result.success) {
                        alert("COMPORT პარამეტრების განახლება ჩაიშალა: უცნობი შეცდომა");
                    }
                } catch (err) {
                    content.innerHTML = `<p style="color:red;">Error applying COMPORT settings.</p>`;
                    updateIndicator(false);
                    alert("შეცდომა COMPORT პარამეტრების განახლებისას: " + err.message);
                }
            };
            trackEventListener(newSetBtn, "click", handler);
        }
    });
}

export function cleanupComportModule() {
    // Clear all setTimeouts
    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    // Clear all event listeners
    clearEventListeners();

    console.log('✅ Comport module cleaned up');
}