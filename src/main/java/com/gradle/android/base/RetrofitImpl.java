package com.gradle.android.base;

import java.util.concurrent.TimeUnit;

import com.gradle.android.Interceptor.CacheInterceptor;
import com.gradle.android.Interceptor.LogInterceptor;
import com.gradle.android.base.HttpClient.CacheType;
import com.gradle.android.converter.StringConverterFactory;
import com.gradle.android.retrofit.OkhttpUtils;
import com.gradle.android.retrofit.ParamService;
import com.gradle.android.retrofit.OkhttpUtils.TrustAllCerts;
import com.gradle.android.retrofit.OkhttpUtils.TrustAllHostnameVerifier;
import com.gradle.java.rxjava.RxjavaUtils;
import com.gradle.java.utils.DateFormatUtil;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Retrofit封装Okhttp的方式进行网络操作
 * 
 * @author Arison
 *
 */
public class RetrofitImpl extends HttpBase {

	public Retrofit retrofit;
	private static RetrofitImpl instance;

	public static RetrofitImpl getInstance() {
		if (instance == null) {
			synchronized (RetrofitImpl.class) {
				if (instance == null) {
					instance = new RetrofitImpl();
				}
			}
		}

		return instance;
	}

	@Override
	public void initClient() {
		// 本类保证初始化一次,减少系统开销
		Builder okBuilder = new OkHttpClient.Builder()
				.connectTimeout(mbuilder.getConnectTimeout(), TimeUnit.SECONDS)
				.readTimeout(mbuilder.getReadTimeout(), TimeUnit.SECONDS)
				.writeTimeout(mbuilder.getWriteTimeout(), TimeUnit.SECONDS)
				.sslSocketFactory(OkhttpUtils.createSSLSocketFactory(), new TrustAllCerts())// 信任所有证书
				.hostnameVerifier(new TrustAllHostnameVerifier());

		LogInterceptor logInterceptor = new LogInterceptor();
		logInterceptor.setBuilder(mbuilder);
		okBuilder.addInterceptor(logInterceptor);
		if (mbuilder.getCacheFileSize()!=0){
			okBuilder.cache(new Cache(mbuilder.getCacheFile(), mbuilder.getCacheFileSize()));
			okBuilder.addInterceptor(new CacheInterceptor(String.valueOf(mbuilder.getCacheTime()),mbuilder.getCacheType()));
		}

		//后期缓存策略改进
		switch (mbuilder.getCacheType()) {
			case CacheType.ONLY_NETWORK:
				  OkhttpUtils.println("CacheType.ONLY_NETWORK");
				  
				break;
			case CacheType.ONLY_CACHED:
	           OkhttpUtils.println("CacheType.ONLY_CACHED");
	           //okBuilder.cache(new Cache(mbuilder.getCacheFile(), mbuilder.getCacheFileSize()));
	       	
				break;
			case CacheType.CACHED_ELSE_NETWORK:
	
				break;
			case CacheType.NETWORK_ELSE_CACHED:
	
				break;
			default:
				break;
		}

		OkHttpClient client = okBuilder.build();
		retrofit = new Retrofit.Builder().client(client)
				.baseUrl(mbuilder.getBaseUrl())
				.addConverterFactory(StringConverterFactory.create())
				.addConverterFactory(GsonConverterFactory.create())
				.addCallAdapterFactory(RxJavaCallAdapterFactory.create())
				.build();
	}

	public <T> T initApi(Class<T> service) {
		return retrofit.create(service);
	}

	@Override
	public void get(HttpClient builder, Subscriber<Object> s) {
		// 局部请求头
		ParamService paramService = initApi(ParamService.class);
		Observable<Object> o = paramService.getParam(builder.getBaseUrl(), builder.getParams(), builder.getHeaders());
		toSubscribe(o, s);
		
	}

	@Override
	public void post(HttpClient builder, Subscriber<Object> s) {
		ParamService paramService = initApi(ParamService.class);
		Observable<Object> o = paramService.postParam(builder.getBaseUrl(), builder.getParams(), builder.getHeaders());
		toSubscribe(o, s);
	}

	private <T> void toSubscribe(Observable<T> o, Subscriber<T> s) {
		o.retryWhen(new Func1<Observable<? extends Throwable>, Observable<?>>() {

			@Override
			public Observable<?> call(Observable<? extends Throwable> t) {
                 OkhttpUtils.println(t);
				return t.flatMap(new Func1<Throwable, Observable<?>>() {
					private int count = 0;
                     
					@Override
					public Observable<?> call(Throwable t) {
						OkhttpUtils.println("重连数：" + mbuilder.getMaxRetryCount() + " 当前数：" + count + " 间隔时间："
								+ mbuilder.getRetryTimeout() + " 当前时间：" + DateFormatUtil.getDateTimeStr());
						if (++count <= mbuilder.getMaxRetryCount()) {
							OkhttpUtils.println("请求异常：" + t.getMessage());
						    Observable<?> ob=	Observable.timer(mbuilder.getRetryTimeout(), TimeUnit.MILLISECONDS);
//						    ob.subscribe(new Subscriber<Object>() {
//
//								@Override
//								public void onCompleted() {
//									
//									
//								}
//
//								@Override
//								public void onError(Throwable e) {
//									OkhttpUtils.println("onError:"+e);
//									
//								}
//
//								@Override
//								public void onNext(Object t) {
//								   OkhttpUtils.println("onNext:"+t);
//									
//								}
//							});
							return ob;
						}
		
						return Observable.error(t);
					}
				});
			}
		}).map(new Func1<T, T>() {

			@Override
			public T call(T t) {
				return (T) t;
			}
		}).subscribeOn(RxjavaUtils.getNamedScheduler("线程1")).subscribe(s);
	}
}