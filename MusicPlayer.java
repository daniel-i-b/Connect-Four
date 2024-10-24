import java.io.File;
import javax.sound.sampled.*;


// Fun class to play music during game
public class MusicPlayer {
    final String jeopardy_theme_file_path = "Music" + System.getProperty("file.separator") +
        "Jeopardy Theme.wav";
    
    Clip music_clip;
    long music_time_position = 0;
    boolean is_paused = false;


    // Starts the music clip or unpauses it
    public void start(String file_path, boolean loop) {
        // If audio has not been loaded 
        if (music_clip == null) {
            File music_file = new File(file_path);
            // Get music
            try (AudioInputStream audio_stream = AudioSystem.getAudioInputStream(music_file)) {
                music_clip = AudioSystem.getClip();
                music_clip.open(audio_stream);
            } 
            catch (Exception e) {
                System.out.println("Music player could not load music :(");
                reset();
            }

            // Loop forever
            if (loop) {
                music_clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            // Start playing
            music_clip.start();
        }
        else {
            // If music has been started, attempt to unpause instead
            unpause();
        }
    }


    // Pauses already playing music. Saves its position so it can resume from where it was paused
    public void pause() {
        if (music_clip != null && music_clip.isRunning()) {
            music_time_position = music_clip.getMicrosecondPosition();
            music_clip.stop();
            is_paused = true;
        }
    }


    // Resumes paused music from where it was paused
    public void unpause() {
        if (music_clip != null && is_paused) {
            music_clip.setMicrosecondPosition(music_time_position);
            music_clip.start();
            is_paused = false;
        }
    }


    // Stops and resets music player
    public void reset() {
        if (music_clip != null) {
            music_clip.stop();
            music_clip.close();
            music_clip = null;
            music_time_position = 0;
            is_paused = false;
        }
    }
}
