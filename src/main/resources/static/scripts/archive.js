async function loadInitialWagonData(netContainer) {
    if (!netContainer) return;

    const targetContainer = netContainer.querySelector("#train-data-container");
    if (!targetContainer) return;
    try {
        const response = await fetch("/showTrains", { method: "POST" });
        if (!response.ok) throw new Error(`Initial load failed: ${response.status}`);

        const html = await response.text();
        targetContainer.innerHTML = html;

    } catch (err) {
        targetContainer.innerHTML = `<p style="color:red;">ჩატვირთვის შეცდომა: ${err.message}</p>`;
    }
}


loadInitialWagonData(document.getElementById("archive-container"));
