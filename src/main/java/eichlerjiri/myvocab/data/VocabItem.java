package eichlerjiri.myvocab.data;

public class VocabItem {

    public final String original;
    public final String translation;
    public final int audioFrom;
    public final int audioTo;

    public VocabItem(String original, String translation, int audioFrom, int audioTo) {
        this.original = original;
        this.translation = translation;
        this.audioFrom = audioFrom;
        this.audioTo = audioTo;
    }
}
