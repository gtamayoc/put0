package gtc.dcc.put0.core.model;

import com.google.gson.annotations.SerializedName;

public class ResponseDetails {

    @SerializedName("responseCode")
    private String responseCode;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Object data;

    // Getters and Setters

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}