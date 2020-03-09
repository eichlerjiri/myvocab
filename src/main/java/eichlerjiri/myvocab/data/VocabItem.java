package eichlerjiri.myvocab.data;

public class VocabItem {

    public final String original;
    public final String translation;
    public final String translationWritten;
    public final int audioFrom;
    public final int audioTo;

    public VocabItem(String original, String translation, String translationWritten, int audioFrom, int audioTo) {
        this.original = original;
        this.translation = translation;
        this.translationWritten = translationWritten;
        this.audioFrom = audioFrom;
        this.audioTo = audioTo;
    }
}
