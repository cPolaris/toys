function timedMsg(msg, time) {
        $.blockUI({
        message: msg,
        css: {
        width: '60%',
        left: '20%',
        border: 'none',
        padding: '15px',
        backgroundColor: '#000',
        '-webkit-border-radius': '10px',
        '-moz-border-radius': '10px',
        opacity: .8,
        color: '#fff'
    } });
    setTimeout($.unblockUI, time);
}

if (window.location.protocol != "https:") {
    timedMsg('Your connection is not secure. Change the address to begin with https://', 4500);
}

var $form = document.querySelectorAll('#signup-form')[0];
$form.addEventListener('submit', function(event) {
    event.stopPropagation();
    event.preventDefault();

    if (window.location.protocol != "https:") {
        timedMsg('Your connection is not secure. Change the address to begin with https://', 4500);
        return false;
    }

    var rawSearch = $('#token').val().split(',');
    var lang, kw;
    if (rawSearch.length > 1) {
        lang = rawSearch[0].toLowerCase().trim();
        LANG = lang;
        kw = rawSearch[1].trim();
        var SUPLANGS = ['en','zh','jp','sv','de','nl','fr','ceb','ru','war','it','es','pl','vi','pt','ko','hi','th'];
        if (SUPLANGS.indexOf(lang) < 0) {
            timedMsg('Unsupported language option', 1500);
            return false;
        }
    } else {
        lang = 'en';
        LANG = lang;
        kw = rawSearch[0].trim();
    }

    $.blockUI({
        message: 'Processing...',
        css: {
        border: 'none',
        padding: '15px',
        backgroundColor: '#000',
        '-webkit-border-radius': '10px',
        '-moz-border-radius': '10px',
        opacity: .8,
        color: '#fff'
    } });

    var destUrl = 'https://abcdefghijklmnopqrstuvwxyz.amazonaws.com/designated/route';
    var req = new XMLHttpRequest();
    req.onload = function () {
        $.unblockUI();
        rsp = JSON.parse(req.responseText);
        if (rsp.suc == 'T') {
            document.getElementById("stylesheet-link").href = 'assets/css/wiki.min.css';
            $('#gradient').remove();
            $('#wrapr').html(rsp.content);
            $('footer').remove();
            $('#main-scr').remove();
            $('#animate-scr').remove();
            listenLinks();
        } else if (rsp.suc == 'F') {
            document.getElementById('wrapr').innerHTML = rsp.content;
            listenLinks();
        } else if (rsp.suc == 'NM') {
            $.unblockUI();
            timedMsg('Your search has no match nor suggestions.', 2000);
        }
    };

    req.onerror = function () {
        console.log(req);
        $.unblockUI();
        timedMsg('Unknown error occured!', 1500);
    };

    req.open('POST', destUrl);
    req.setRequestHeader('Content-Type', 'application/json');
    req.timeout = 9999;
    req.ontimeout = function () {
        timedMsg('Reqeust timeout. Consider changing language option', 2500);
    }
    var to_send = {"title": kw, "lang": lang};
    req.send(JSON.stringify(to_send));
});

var $about = document.querySelectorAll('#click-about')[0];
$about.addEventListener('click', function(event) {
    $.blockUI({
            message: 'Open source components:<br><br><a href="http://codepen.io/quasimondo/pen/lDdrF">Animated Background Gradient</a><br><a href="https://github.com/malsup/blockui/">jQuery BlockUI</a><br><a href="http://html5up.net">HTML5Up templetes</a><br><a href="http://brick.im">Brick fonts</a>',
            css: {
            width: '80%',
            left: '10%',
            border: 'none',
            padding: '15px',
            backgroundColor: '#000',
            '-webkit-border-radius': '10px',
            '-moz-border-radius': '10px',
            opacity: .8,
            color: '#fff'
        } });
    $('.blockOverlay').attr('title','Click to unblock').click($.unblockUI);
});

var $help = document.querySelectorAll('#click-help')[0];
$help.addEventListener('click', function(event) {
    $.blockUI({
            message: 'Usage:<br>&lt;language option&gt;, &lt;search keyword&gt;<br>e.g. zh, 人工智能史<br>Omit language option to search English<br>Some available options: en, zh, jp, de, ru, fr, ko, es, pt, sv',
            css: {
            width: '80%',
            left: '10%',
            border: 'none',
            padding: '15px',
            backgroundColor: '#000',
            '-webkit-border-radius': '10px',
            '-moz-border-radius': '10px',
            opacity: .8,
            color: '#fff'
        } });
    $('.blockOverlay').attr('title','Click to unblock').click($.unblockUI);
});
