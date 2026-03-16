package com.uncorrelated.p15;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.UnsupportedAudioFileException;

/*
    java.applet.AudioClipがobsoletedされたので、代替コード
 */
public class SoundEffect {

    private byte[] audioData;
    private AudioFormat audioFormat;
    private int audioDataLength;

    public SoundEffect(String fileName) throws UnsupportedAudioFileException, IOException {
	load(fileName);
    }
    
    private void load(String fileName) throws UnsupportedAudioFileException, IOException {
	InputStream is = Puzzle15.class.getResourceAsStream(fileName);
	if (is == null) {
	    System.err.println("Resource not found: " + fileName);
	    throw new NullPointerException();
	}
	// jarにするときはBufferedInputStreamを挟まないとjava.io.IOException: mark/reset not supportedになる
	AudioInputStream sourceStream = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
	AudioFormat baseFormat = sourceStream.getFormat();

	// ULAW/ALAWなどの圧縮形式を PCM_SIGNED に変換するための指定
	audioFormat = new AudioFormat(
		AudioFormat.Encoding.PCM_SIGNED,
		baseFormat.getSampleRate(), // 8012.0 Hz
		16, // 8bitから16bitへ拡張して互換性を高める
		baseFormat.getChannels(), // mono
		baseFormat.getChannels() * 2, // 16bit(2bytes) * channels
		baseFormat.getSampleRate(),
		false // little endian
	);

	AudioInputStream pcmStream = AudioSystem.getAudioInputStream(audioFormat, sourceStream);
	
	long length = pcmStream.getFrameLength() * audioFormat.getFrameSize();
	audioData = new byte[(int) length];
	audioDataLength = (int) length;

	int bytesRead = 0;
	int offset = 0;
	while (offset < audioData.length
		&& (bytesRead = pcmStream.read(audioData, offset, audioData.length - offset)) != -1) {
	    offset += bytesRead;
	}
    }

    public void play() {
	if (audioData == null) {
	    return;
	}
	try {
	    final Clip clip = AudioSystem.getClip();
	    clip.open(audioFormat, audioData, 0, audioDataLength);
	    clip.addLineListener(new LineListener() {
		public void update(LineEvent event) {
		    if (event.getType() == LineEvent.Type.STOP) {
			clip.close();
		    }
		}
	    });
	    clip.start();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
