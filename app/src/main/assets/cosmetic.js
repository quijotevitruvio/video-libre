(function () {
  if (window.__tvfutClean) {
    return;
  }
  window.__tvfutClean = true;

  try {
    window.open = function () {
      return null;
    };
  } catch (e) {
  }

  var selectors = [
    'iframe[src*="doubleclick.net"]',
    'iframe[src*="googlesyndication"]',
    'iframe[src*="googleadservices"]',
    'iframe[src*="imasdk"]',
    'iframe[id^="google_ads_iframe"]',
    'iframe[name^="google_ads_iframe"]',
    'ins.adsbygoogle',
    '[data-ad-slot]',
    '[data-google-query-id]',
    '[id^="div-gpt-ad"]',
    '[id^="google_ads"]',
    '[class*="adsbygoogle"]',
    '[id*="taboola"]',
    '[class*="taboola"]',
    '[id*="outbrain"]',
    '[class*="outbrain"]',
    '[class*="ad-overlay"]',
    '[class*="ad_overlay"]',
    '[class*="overlay-ad"]',
    '[class*="ad-banner"]',
    '[id*="ad-banner"]',
    '[class*="banner-ad"]',
    '[class*="ad-container"]',
    '[id*="ad-container"]',
    '[class*="ad-wrapper"]',
    '[class*="ad-slot"]',
    '[class*="ad-unit"]',
    '[class*="video-ad"]',
    '[class*="ad-video"]',
    '[class*="popunder"]',
    '[id*="popunder"]',
    '[class*="pop-under"]',
    '[class*="sponsored"]',
    '[aria-label="Advertisement"]',
    '[aria-label="Publicidad"]'
  ];

  function hide(element) {
    try {
      element.style.setProperty('display', 'none', 'important');
    } catch (e) {
      element.style.display = 'none';
    }
  }

  function clean() {
    for (var i = 0; i < selectors.length; i++) {
      var list;
      try {
        list = document.querySelectorAll(selectors[i]);
      } catch (e) {
        continue;
      }
      for (var j = 0; j < list.length; j++) {
        hide(list[j]);
      }
    }
  }

  clean();

  var pending = false;
  function schedule() {
    if (pending) {
      return;
    }
    pending = true;
    setTimeout(function () {
      pending = false;
      clean();
    }, 400);
  }

  try {
    var observer = new MutationObserver(schedule);
    observer.observe(document.documentElement || document, {
      childList: true,
      subtree: true
    });
  } catch (e) {
  }
})();
