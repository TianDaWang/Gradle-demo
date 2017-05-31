package com.gradle.test.http;

import com.gradle.android.base.HttpClient;
import com.gradle.android.base.RetrofitImpl;
import com.gradle.android.subscriber.NetResquestSubscriber;
import com.gradle.android.subscriber.SubscriberOnNextListener;

/**
 * 测试网络请求
 * Created by Arison on 2017/5/31.
 */
public class testHttp {
    public static final String BASE_URL="http://192.168.253.200:8080/Chapter/";
    
    /**
      * @desc:
      * @author：Arison on 2017/5/31
      */
    public static void main(String arg[]){
        HttpClient httpClient=new HttpClient.Builder()
                .url(BASE_URL)
                .httpBase(RetrofitImpl.getInstance())
                .build();


        httpClient.Api().send(new HttpClient.Builder()
        .url("json")
        .add("12","12")
        //.header("12","12")
        .build(),new NetResquestSubscriber<>(new SubscriberOnNextListener<Object>() {
            @Override
            public void onNext(Object o) {

            }
        }));
    }
}
