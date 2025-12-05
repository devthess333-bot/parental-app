<?php
$apk_file = 'downloads/app-debug.apk';

if (file_exists($apk_file)) {
    header('Content-Type: application/vnd.android.package-archive');
    header('Content-Disposition: attachment; filename="ParentalControl.apk"');
    header('Content-Length: ' . filesize($apk_file));
    readfile($apk_file);
    exit;
} else {
    echo "APK file not found!";
}
?>
