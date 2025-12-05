<?php
// upload.php - Test endpoint για voice recording
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');

// Upload directory
$uploadDir = __DIR__ . '/uploads/';
if (!file_exists($uploadDir)) {
    mkdir($uploadDir, 0777, true);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_FILES['audio_file'])) {
        $file = $_FILES['audio_file'];
        
        if ($file['error'] === UPLOAD_ERR_OK) {
            $filename = 'recording_' . date('Ymd_His') . '.mp3';
            $filepath = $uploadDir . $filename;
            
            if (move_uploaded_file($file['tmp_name'], $filepath)) {
                echo json_encode([
                    'status' => 'success',
                    'message' => 'File uploaded',
                    'filename' => $filename,
                    'url' => 'http://localhost/parental/call-recording-test/backend/uploads/' . $filename
                ]);
            } else {
                echo json_encode(['status' => 'error', 'message' => 'Upload failed']);
            }
        } else {
            echo json_encode(['status' => 'error', 'message' => 'File error']);
        }
    } else {
        echo json_encode(['status' => 'error', 'message' => 'No file']);
    }
} else {
    echo json_encode(['status' => 'error', 'message' => 'POST only']);
}
?>
