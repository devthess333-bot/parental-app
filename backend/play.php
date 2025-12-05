<?php
// play.php - Simple player για τα recordings
$uploadDir = __DIR__ . '/uploads/';
?>
<!DOCTYPE html>
<html lang="el">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Call Recording Test Player</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        
        h1 {
            color: #333;
            border-bottom: 2px solid #4CAF50;
            padding-bottom: 10px;
        }
        
        .recording {
            border: 1px solid #ddd;
            padding: 15px;
            margin: 15px 0;
            border-radius: 5px;
            background: #fafafa;
        }
        
        audio {
            width: 100%;
            margin: 10px 0;
        }
        
        .controls {
            margin-top: 10px;
        }
        
        button {
            background: #4CAF50;
            color: white;
            border: none;
            padding: 8px 15px;
            border-radius: 3px;
            cursor: pointer;
            margin-right: 5px;
        }
        
        button:hover {
            background: #45a049;
        }
        
        .empty {
            text-align: center;
            padding: 40px;
            color: #999;
            font-size: 18px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>📞 Call Recording Test Player</h1>
        <p>Test interface για το parental control app - Voice Recording Feature</p>
        
        <?php
        // Get all audio files
        $audioFiles = [];
        $extensions = ['mp3', 'wav', 'ogg', 'm4a'];
        
        foreach ($extensions as $ext) {
            $files = glob($uploadDir . "*.$ext");
            $audioFiles = array_merge($audioFiles, $files);
        }
        
        // Sort by modification time (newest first)
        usort($audioFiles, function($a, $b) {
            return filemtime($b) - filemtime($a);
        });
        
        if (empty($audioFiles)) {
            echo '<div class="empty">🚫 Δεν βρέθηκαν ηχογραφήσεις. Κάνε μια test κλήση από το app.</div>';
        } else {
            echo '<p>Βρέθηκαν ' . count($audioFiles) . ' ηχογραφήσεις</p>';
            
            foreach ($audioFiles as $file) {
                $filename = basename($file);
                $fileDate = date('Y-m-d H:i:s', filemtime($file));
                
                echo '<div class="recording">';
                echo '<h3>' . htmlspecialchars($filename) . '</h3>';
                echo '<p>📅 ' . $fileDate . '</p>';
                
                echo '<audio controls>';
                echo '<source src="uploads/' . urlencode($filename) . '" type="audio/mpeg">';
                echo 'Your browser does not support the audio element.';
                echo '</audio>';
                
                echo '<div class="controls">';
                echo '<button onclick="window.open(\'uploads/' . urlencode($filename) . '\', \'_blank\')">📥 Download</button>';
                echo '</div>';
                echo '</div>';
            }
        }
        ?>
    </div>
</body>
</html>
