window.addEventListener('load', registerServiceWorker, false);

function registerServiceWorker() {
    if ('serviceWorker' in navigator) {
        document.getElementById('service-worker').append('y');
        navigator.serviceWorker.register('/sw.js').then(initialiseState);
    } else {
        document.getElementById('service-worker').append('n');
        console.warn('Service workers are not supported in this browser.');
    }
}

function initialiseState() {
    console.log('Service worker is registered.');

    if (!('showNotification' in ServiceWorkerRegistration.prototype)) {
        console.warn('Notifications aren\'t supported.');
        document.getElementById('show-notification').append('n');
        return;
    } else {
        document.getElementById('show-notification').append('y');
    }

    if (Notification.permission === 'denied') {
        console.warn('The user has blocked notifications.');
        document.getElementById('notification-permission').append('n');
        return;
    } else {
        document.getElementById('notification-permission').append('y');
    }

    if (!('PushManager' in window)) {
        console.warn('Push messaging isn\'t supported.');
        document.getElementById('push-manager').append('n');
        return;
    } else {
        document.getElementById('push-manager').append('y');
    }

    //var readyPromise = navigator.serviceWorker.ready;
    var readyPromise = navigator.serviceWorker.getRegistration('./');
    readyPromise.then(function (serviceWorkerRegistration) {
        console.log('Service worker is ready.');
        document.getElementById('service-worker-ready').append('y');

        serviceWorkerRegistration.pushManager.getSubscription().then(function (subscription) {
            console.log('Got subscription');
            document.getElementById('subscription-ready').append('y');

            if (!subscription) {
                subscribe();

                return;
            }

            // Keep your server in sync with the latest subscriptionId
            sendSubscriptionToServer(subscription);
        })
        .catch(function (err) {
            console.warn('Error during getSubscription()', err);
        });
    });
}

function subscribe() {
    const publicKey = base64UrlToUint8Array('BAPGG2IY3Vn48d_H8QNuVLRErkBI0L7oDOOCAMUBqYMTMTzukaIAuB5OOcmkdeRICcyQocEwD-oxVc81YXXZPRY');

    //var readyPromise = navigator.serviceWorker.ready;
    var readyPromise = navigator.serviceWorker.getRegistration('./');

    readyPromise.then(function (serviceWorkerRegistration) {
        serviceWorkerRegistration.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: publicKey
        })
        .then(function (subscription) {
            return sendSubscriptionToServer(subscription);
        })
        .catch(function (e) {
            if (Notification.permission === 'denied') {
                console.warn('Permission for Notifications was denied');
            } else {
                console.error('Unable to subscribe to push.', e);
            }
        });
    });
}

function sendSubscriptionToServer(subscription) {
    var key = subscription.getKey ? subscription.getKey('p256dh') : '';
    var auth = subscription.getKey ? subscription.getKey('auth') : '';

    document.getElementById('subscription').value = JSON.stringify(subscription);

    console.log({
        endpoint: subscription.endpoint,
        key: key ? btoa(String.fromCharCode.apply(null, new Uint8Array(key))) : '',
        auth: auth ? btoa(String.fromCharCode.apply(null, new Uint8Array(auth))) : ''
    });

    return Promise.resolve();

    // Normally, you would actually send the subscription to the server:
    return fetch('/profile/subscription', {
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json'
        },
        method: 'POST',
        body: JSON.stringify({
            endpoint: subscription.endpoint,
            key: key ? btoa(String.fromCharCode.apply(null, new Uint8Array(key))) : '',
            auth: auth ? btoa(String.fromCharCode.apply(null, new Uint8Array(auth))) : ''
        })
    });
}

function base64UrlToUint8Array(base64UrlData) {
    const padding = '='.repeat((4 - base64UrlData.length % 4) % 4);
    const base64 = (base64UrlData + padding)
        .replace(/\-/g, '+')
        .replace(/_/g, '/');

    const rawData = atob(base64);
    const buffer = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
        buffer[i] = rawData.charCodeAt(i);
    }

    return buffer;
}

window.addEventListener('load', function () {
    var button = document.getElementById('send');
    button.addEventListener('click', function () {
        var subscription = document.getElementById('subscription').value;

        let formData = new FormData();
        formData.append('subscriptionJson', subscription);

        fetch('/send', {
            method: 'POST',
            body: formData
        });
    });
});

// Use Broadcast API to listen for messages from the service worker
var broadcast = new BroadcastChannel('message-received');

broadcast.onmessage = function (event) {
  var li = document.createElement('li');
  li.innerText = event.data.text;
  document.getElementById('messages').append(li);
};
