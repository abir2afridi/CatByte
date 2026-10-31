#![cfg_attr(
    all(not(debug_assertions), target_os = "windows"),
    windows_subsystem = "windows"
)]

use std::path::PathBuf;
use tauri::{Manager, State};
use tauri_plugin_dialog::DialogExt;
use tauri_plugin_fs::FsExt;
use tauri_plugin_shell::ShellExt;
use tauri_plugin_opener::OpenerExt;
use serde::{Deserialize, Serialize};
use std::sync::Mutex;
use std::path::PathBuf;

// Project structure for backend
#[derive(Debug, Serialize, Deserialize, Clone)]
struct VideoProject {
    id: String,
    name: String,
    created_at: i64,
    updated_at: i64,
    settings: ProjectSettings,
    tracks: Vec<Track>,
    media_pool: Vec<MediaAsset>,
    duration: i64,
    version: i32,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct ProjectSettings {
    width: u32,
    height: u32,
    frame_rate: u32,
    background_color: String,
    sample_rate: u32,
    channels: u32,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct Track {
    id: String,
    name: String,
    track_type: String,
    enabled: bool,
    locked: bool,
    muted: bool,
    solo: bool,
    height: u32,
    order: u32,
    clips: Vec<Clip>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct Clip {
    id: String,
    track_id: String,
    asset_id: String,
    name: String,
    start: i64,
    end: i64,
    source_start: i64,
    source_end: i64,
    transform: ClipTransform,
    opacity: f32,
    volume: f32,
    speed: f32,
    reverse: bool,
    effects: Vec<ClipEffect>,
    keyframes: Vec<Keyframe>,
    text_content: Option<String>,
    text_style: Option<TextStyle>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct ClipTransform {
    x: f32,
    y: f32,
    scale_x: f32,
    scale_y: f32,
    rotation: f32,
    anchor_x: f32,
    anchor_y: f32,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct ClipEffect {
    id: String,
    effect_type: String,
    enabled: bool,
    params: serde_json::Value,
    keyframes: Vec<Keyframe>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct Keyframe {
    id: String,
    property: String,
    frame: i64,
    value: serde_json::Value,
    easing: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct MediaAsset {
    id: String,
    name: String,
    asset_type: String,
    file_path: String,
    thumbnail_path: Option<String>,
    duration: i64,
    width: u32,
    height: u32,
    frame_rate: u32,
    sample_rate: Option<u32>,
    channels: Option<u32>,
    file_size: u64,
    metadata: serde_json::Value,
    created_at: i64,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct TextStyle {
    font_family: String,
    font_size: u32,
    font_weight: u32,
    color: ColorValue,
    stroke_color: Option<ColorValue>,
    stroke_width: f32,
    alignment: String,
    line_height: f32,
    letter_spacing: f32,
    transform: String,
    animation_in: Option<TextAnimation>,
    animation_out: Option<TextAnimation>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct ColorValue {
    r: f32,
    g: f32,
    b: f32,
    a: f32,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
struct TextAnimation {
    animation_type: String,
    duration: i64,
    easing: String,
    params: serde_json::Value,
}

// App state
struct AppState {
    current_project: Mutex<Option<VideoProject>>,
    recent_projects: Mutex<Vec<String>>,
}

// Commands
#[tauri::command]
async fn create_project(name: String, settings: Option<ProjectSettings>) -> Result<VideoProject, String> {
    let project = VideoProject {
        id: uuid::Uuid::new_v4().to_string(),
        name,
        created_at: chrono::Utc::now().timestamp_millis(),
        updated_at: chrono::Utc::now().timestamp_millis(),
        settings: settings.unwrap_or(ProjectSettings {
            width: 1920,
            height: 1080,
            frame_rate: 30,
            background_color: "#000000".to_string(),
            sample_rate: 48000,
            channels: 2,
        }),
        tracks: vec![
            Track {
                id: uuid::Uuid::new_v4().to_string(),
                name: "Video 1".to_string(),
                track_type: "video".to_string(),
                enabled: true,
                locked: false,
                muted: false,
                solo: false,
                height: 80,
                order: 0,
                clips: vec![],
            },
            Track {
                id: uuid::Uuid::new_v4().to_string(),
                name: "Audio 1".to_string(),
                track_type: "audio".to_string(),
                enabled: true,
                locked: false,
                muted: false,
                solo: false,
                height: 60,
                order: 1,
                clips: vec![],
            },
        ],
        media_pool: vec![],
        duration: 30 * 30, // 30 seconds at 30fps
        version: 1,
    };
    Ok(project)
}

#[tauri::command]
async fn save_project(project: VideoProject, path: String) -> Result<(), String> {
    let json = serde_json::to_string_pretty(&project).map_err(|e| e.to_string())?;
    std::fs::write(&path, json).map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
async fn load_project(path: String) -> Result<VideoProject, String> {
    let json = std::fs::read_to_string(&path).map_err(|e| e.to_string())?;
    let project: VideoProject = serde_json::from_str(&json).map_err(|e| e.to_string())?;
    Ok(project)
}

#[tauri::command]
async fn import_media(file_path: String) -> Result<MediaAsset, String> {
    let path = PathBuf::from(&file_path);
    let metadata = std::fs::metadata(&path).map_err(|e| e.to_string())?;
    
    let mime = mime_guess::from_path(&path).first_or_octet_stream();
    let asset_type = if mime.type_() == "video" {
        "video"
    } else if mime.type_() == "audio" {
        "audio"
    } else if mime.type_() == "image" {
        "image"
    } else {
        "unknown"
    };

    // Get video info using ffprobe (simplified)
    let (duration, width, height, frame_rate) = if asset_type == "video" {
        get_video_info(&file_path).await.unwrap_or((30 * 30, 1920, 1080, 30))
    } else if asset_type == "image" {
        let img = image::open(&path).map_err(|e| e.to_string())?;
        (5 * 30, img.width(), img.height(), 30) // 5 seconds default for images
    } else {
        (30 * 30, 0, 0, 30) // Audio default
    };

    Ok(MediaAsset {
        id: uuid::Uuid::new_v4().to_string(),
        name: path.file_name().unwrap().to_string_lossy().to_string(),
        asset_type: asset_type.to_string(),
        file_path,
        thumbnail_path: None,
        duration,
        width,
        height,
        frame_rate,
        sample_rate: if asset_type == "audio" { Some(48000) } else { None },
        channels: if asset_type == "audio" { Some(2) } else { None },
        file_size: metadata.len(),
        metadata: serde_json::json!({}),
        created_at: chrono::Utc::now().timestamp_millis(),
    })
}

async fn get_video_info(path: &str) -> Result<(i64, u32, u32, u32), String> {
    // In a real implementation, use ffprobe to get actual video metadata
    // For now, return defaults
    Ok((30 * 30, 1920, 1080, 30))
}

#[tauri::command]
async fn generate_thumbnail(video_path: String, output_path: String, time: String) -> Result<(), String> {
    // Use ffmpeg to generate thumbnail
    let output = std::process::Command::new("ffmpeg")
        .args([
            "-y",
            "-ss", &time,
            "-i", &video_path,
            "-vframes", "1",
            "-q:v", "2",
            &output_path,
        ])
        .output()
        .map_err(|e| e.to_string())?;

    if !output.status.success() {
        return Err(String::from_utf8_lossy(&output.stderr).to_string());
    }
    Ok(())
}

#[tauri::command]
async fn export_video(
    project: VideoProject,
    output_path: String,
    settings: ExportSettings,
) -> Result<(), String> {
    // This would use FFmpeg to render the final video
    // For now, just create a placeholder
    println!("Exporting video to: {}", output_path);
    println!("Settings: {:?}", settings);
    
    // In a real implementation:
    // 1. Generate filter complex for all clips, effects, transitions
    // 2. Run FFmpeg with the filter graph
    // 3. Handle progress reporting
    
    Ok(())
}

#[derive(Debug, Serialize, Deserialize)]
struct ExportSettings {
    format: String,
    codec: String,
    width: u32,
    height: u32,
    frame_rate: u32,
    bitrate: u32,
    audio_bitrate: u32,
    audio_sample_rate: u32,
    audio_channels: u32,
    quality: String,
    hardware_acceleration: bool,
    two_pass: bool,
    range: String,
    in_point: i64,
    out_point: i64,
}

#[tauri::command]
async fn get_recent_projects() -> Result<Vec<String>, String> {
    // Load from config file
    Ok(vec![])
}

#[tauri::command]
async fn add_recent_project(path: String) -> Result<(), String> {
    // Save to config file
    Ok(())
}

#[tauri::command]
async fn open_file_dialog() -> Result<Option<String>, String> {
    // This would be handled by the dialog plugin on the frontend
    Ok(None)
}

#[tauri::command]
async fn save_file_dialog() -> Result<Option<String>, String> {
    Ok(None)
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_opener::init())
        .manage(AppState {
            current_project: Mutex::new(None),
            recent_projects: Mutex::new(vec![]),
        })
        .invoke_handler(tauri::generate_handler![
            create_project,
            save_project,
            load_project,
            import_media,
            generate_thumbnail,
            export_video,
            get_recent_projects,
            add_recent_project,
            open_file_dialog,
            save_file_dialog,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}