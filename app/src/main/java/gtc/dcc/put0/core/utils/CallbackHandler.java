package gtc.dcc.put0.core.utils;

public interface CallbackHandler<T> {
    void onSuccess(T result);
    void onFailure(Throwable throwable);
}