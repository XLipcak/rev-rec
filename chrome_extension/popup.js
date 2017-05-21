/**
 * Get the current URL.
 *
 * @param {function(string)} callback - called when the URL of the current tab
 *   is found.
 */
function getCurrentTabUrl(callback) {
    // Query filter to be passed to chrome.tabs.query - see
    // https://developer.chrome.com/extensions/tabs#method-query
    var queryInfo = {
        active: true,
        currentWindow: true
    };

    chrome.tabs.query(queryInfo, function (tabs) {
        // chrome.tabs.query invokes the callback with a list of tabs that match the
        // query. When the popup is opened, there is certainly a window and at least
        // one tab, so we can safely assume that |tabs| is a non-empty array.
        // A window can only have one active tab at a time, so the array consists of
        // exactly one tab.
        var tab = tabs[0];

        // A tab is a plain object that provides information about the tab.
        // See https://developer.chrome.com/extensions/tabs#type-Tab
        var url = tab.url;

        // tab.url is only available if the "activeTab" permission is declared.
        // If you want to see the URL of other tabs (e.g. after removing active:true
        // from |queryInfo|), then the "tabs" permission is required to see their
        // "url" properties.
        console.assert(typeof url == 'string', 'tab.url should be a string');

        callback(url);
    });
}

/**
 * @param {string} searchUrl - Change request URL
 */
function getActualUrl(searchUrl) {

    var x = new XMLHttpRequest();
    var changeId = '';
    var url = searchUrl;
    var parts = url.split("/");
    if (parts[parts.length - 1].length == 0) {
        changeId = parts[parts.length - 2];
    } else {
        changeId = parts[parts.length - 1];
    }

    x.open('GET', 'http://localhost:6060/api/reviewers-recommendation?gerritChangeNumber=' + changeId, false);

    x.send();

    var content = '';
    var json_obj = JSON.parse(x.responseText);

    for (x = 0; x < json_obj.length; x++) {
        var y = x + 1;
        content += '<tr><th>' + y + '</th><th>' + json_obj[x].name + '</th>' + '<td><img src="' + json_obj[x].avatar + '" alt="" border=3 height=50 width=50></img></td>' + '</tr>';
    }

    document.getElementById('status').innerHTML = content;
    console.log('Done!');
}

function renderStatus(statusText) {
    document.getElementById('status').textContent = statusText;
}

document.addEventListener('DOMContentLoaded', function () {
    getCurrentTabUrl(function (url) {
        renderStatus('Performing recommendation for ' + url);

        getActualUrl(url);
    });
});
