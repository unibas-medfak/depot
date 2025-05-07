@param gg.jte.Content content

<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <script src="https://unpkg.com/htmx.org@2.0.4"></script>
    <script src="/html5-qrcode.min.js"></script>
    <script src="/scan.js"></script>
    <title>Depot</title>
</head>
<body>
<div id="qr-reader" style="width:500px"></div>
<div id="qr-reader-results"></div>
    ${content}
</body>
</html>