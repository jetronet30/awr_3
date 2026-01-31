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

export function initTcpModule() {
    const netContainer = document.querySelector("#tcp-settings-container");
    const content = document.querySelector("main.content");

    if (!netContainer || !content) {
        return;
    }

    function updateIndicator(success) {
        const indicator = document.querySelector("#tcp-settings-indicator");
        if (indicator) {
            indicator.style.transition = "background-color 0.5s ease";
            indicator.style.backgroundColor = success ? "#00cc66" : "#ff3333";
            const timeoutId = setTimeout(() => {
                indicator.style.backgroundColor = "";
            }, 5000);
            intervalIds.add(timeoutId); // Track timeout
        }
    }

    /** --- 🌐 reboot ფორმა --- */
    const setWRForm = netContainer.querySelector('form[action="/tcp-setting-reboot"]');
    const setWRBtn = setWRForm?.querySelector("#tcp-settings-reboot-btn");

    if (setWRForm && setWRBtn) {
        setWRBtn.replaceWith(setWRBtn.cloneNode(true));
        const newSetWRBtn = netContainer.querySelector("#tcp-settings-reboot-btn");

        const handler = async (e) => {
            e.preventDefault();
            const formData = new FormData(setWRForm);
            try {
                const response = await fetch(setWRForm.action, { method: "POST", body: formData });
                if (!response.ok) throw new Error(`Server error: ${response.status}`);
                const html = await response.text();
                content.innerHTML = html;
                if (content.querySelector("#tcp-settings-container")) {
                    const mod = await import(`/scripts/tcp.js?v=${Date.now()}`);
                    mod.initTcpModule();
                }
                updateIndicator(true);
            } catch (err) {
                content.innerHTML = `<p style="color:red;">Error applying TCPsettings.</p>`;
                updateIndicator(false);
                alert("შეცდომა TCP პარამეტრების გამოყენებისას: " + err.message);
            }
        };
        trackEventListener(newSetWRBtn, "click", handler);
    }

    /** --- ⚙️ setTcp ფორმები --- */
    const lanSettingForms = netContainer.querySelectorAll(".tcp-settings-from");
    lanSettingForms.forEach(form => {
        const setBtn = form.querySelector(".tcp-settings-btn");
        if (setBtn) {
            setBtn.replaceWith(setBtn.cloneNode(true));
            const newSetBtn = form.querySelector(".tcp-settings-btn");

            const handler = async (e) => {
                e.preventDefault();
                const formData = new FormData(form);
                try {
                    const response = await fetch(form.action, { method: "POST", body: formData });
                    if (!response.ok) throw new Error(`Server error: ${response.status}`);
                    const result = await response.json();
                    updateIndicator(result.success);

                    if (!result.success && result.message) {
                        alert("TCP პარამეტრების განახლება ჩაიშალა: " + result.message);
                    } else if (!result.success) {
                        alert("TCP პარამეტრების განახლება ჩაიშალა: უცნობი შეცდომა");
                    }
                } catch (err) {
                    content.innerHTML = `<p style="color:red;">Error applying CAM settings.</p>`;
                    updateIndicator(false);
                    alert("შეცდომა TCP პარამეტრების განახლებისას: " + err.message);
                }
            };
            trackEventListener(newSetBtn, "click", handler);
        }
    });
}

export function cleanupTcpModule() {
    // Clear all setTimeouts
    intervalIds.forEach(id => clearTimeout(id));
    intervalIds.clear();

    // Clear all event listeners
    clearEventListeners();

    console.log('✅ TCP module cleaned up');
}