package cn.com.cpy.eims;

/**
 * @author chenpanyu
 * @version 1.0
 * @since 2018/8/26 23:28
 */
public class Greeting {
    private final long id;
    private final String content;

    public Greeting(long id, String content) {
        this.id = id;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }
}
