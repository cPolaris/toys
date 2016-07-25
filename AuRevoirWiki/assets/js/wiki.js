var LANG = 'en';

function listenLinks() {
    $('a').click(function(event) {
        var ref = $(this).attr('href');
        if (ref && ref.slice(0, 6) == '/wiki/') {
            event.preventDefault();
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

            var kw = ref.slice(6);
            var destUrl = 'https://abcdefghijklmnopqrstuvwxyz.amazonaws.com/designated/route';
            var req = new XMLHttpRequest();
            req.onload = function () {
                $.unblockUI();
                rsp = JSON.parse(req.responseText);
                if (rsp.suc == 'T') {
                    $('#wrapr').html(rsp.content);
                    listenLinks();
                } else if (rsp.suc == 'NM') {
                    $.unblockUI();
                    $.blockUI({
                        message: 'Article not found. Consider changing language.',
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
                    setTimeout($.unblockUI, 2000);
                }
            };

            req.onerror = function () {
                console.log(req);
                $.unblockUI();
                $.blockUI({
                    message: 'Unknown error occured!',
                    css: {
                    border: 'none',
                    padding: '15px',
                    backgroundColor: '#000',
                    '-webkit-border-radius': '10px',
                    '-moz-border-radius': '10px',
                    opacity: .8,
                    color: '#fff'
                } });
                setTimeout($.unblockUI, 3000);
            };

            req.open('POST', destUrl);
            req.setRequestHeader('Content-Type', 'application/json');
            var to_send = {"title": kw, "lang": LANG};
            console.log(to_send);
            req.send(JSON.stringify(to_send));
        }
    });
}