let intervalId = null;

// ინიციალიზაციის ფუნქცია
export function initSuperadminModule() {
    // ვიწყებთ interval ლოგიკას
    intervalId = setInterval(() => {
        console.log("AAAAAAAAA");
    }, 1000);
}

// გაწმენდის ფუნქცია
export function cleanupSuperadminModule() {
    if (intervalId) {
        clearInterval(intervalId);
        intervalId = null;
        console.log('✅ Superadmin module cleaned up');
    }
}