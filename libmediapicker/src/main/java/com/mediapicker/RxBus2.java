package com.mediapicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * RxBus 不是一个库，而是一种模式，它的思想是使用 RxJava 来实现了EventBus 。
 * 用 RxBus 来替代 EventBus 主要是为了减少程序对第三方库的引用
 */
public class RxBus2 {

    private static final String TAG = RxBus2.class.getSimpleName();
    private static RxBus2 instance;
    private final Subject<Object, Object> subject;

    private static final int MAIN            = 0x01;
    private static final int IO              = 0x02;
    private static final int NEWTHREAD       = 0x03;
    private static final int COMPUTATION     = 0x04;
    private static final int IMMIDIATE       = 0x05;
    private static final int TRAMPOLINE      = 0x06;

    private final Map<Object, List<Subscription>> subscriptiontMap;
    private Object toSubscriber = null;
    private String toTag = null;

    private RxBus2() {
        subject = new SerializedSubject<>(PublishSubject.create());
        subscriptiontMap = new HashMap<>();
    }

    // 单例RxBus
    private static RxBus2 getDefault() {
        if (instance == null) {
            synchronized (RxBus2.class) {
                if (instance == null) {
                    instance = new RxBus2();
                }
            }
        }
        return instance;
    }

    /**
     * 给所有订阅者发送数据
     * @param event 递送的事件
     */
    private static void post(Object event) {
        getDefault().toSubscriber = null;
        getDefault().toTag = null;
        getDefault().subject.onNext(event);
    }

    /**
     * 在指定时间后，给所有订阅者发送数据
     * @param event 递送的事件
     * @param delay 延迟的时间
     * @param timeUnit 时间单位
     */
    public static void postDelay(final Object event, long delay, TimeUnit timeUnit) {
        Observable.timer(delay, timeUnit)
            .subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    post(event);
                }
            });
    }

    /**
     * 给指定的对象中的所有订阅者发送数据
     * @param event 递送的事件
     * @param subscriber 指定订阅者对象
     */
    private static void post(Object event, Object subscriber) {
        getDefault().toTag = null;
        getDefault().toSubscriber = subscriber;
        getDefault().subject.onNext(event);
    }

    public static void postDelay(final Object event, final Object subscriber, long delay, TimeUnit timeUnit) {
        Observable.timer(delay, timeUnit)
            .subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    post(event, subscriber);
                }
            });
    }

    /**
     * 给某一类别的订阅者发送数据
     * @param event 递送的事件
     * @param tag 标记订阅者的类别
     */
    public static void post(Object event, String tag) {
        getDefault().toSubscriber = null;
        getDefault().toTag = tag;
        getDefault().subject.onNext(event);
    }

    public static void postDelay(final Object event, final String tag, long delay, TimeUnit timeUnit) {
        Observable.timer(delay, timeUnit)
            .subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    post(event, tag);
                }
            });
    }

    /**
     * 取消订阅
     * @param subscriber 订阅者对象
     */
    public static void unSubscibe(Object subscriber) {
        if (getDefault().subscriptiontMap.containsKey(subscriber)) {
            for (Subscription subscription : getDefault().subscriptiontMap.get(subscriber)) {
                if (subscription != null && !subscription.isUnsubscribed()) {
                    subscription.unsubscribe();
                }
            }
        }
    }

    private static <T> Observable<T> toObservable(Class<T> eventType) {
        return getDefault().subject.ofType(eventType);
    }

    /**
     * 订阅事件
     * @param subscriber 订阅者对象
     * @param type 事件的类型
     * @param threadMode 线程模式，处理事件时所在的线程
     * @param action 事件的处理程序
     * @param tag 标记订阅者的类别
     */
    private static <T> void subscribe(final Object subscriber, Class<T> type, int threadMode, Action1<T> action, final String tag) {
        if (subscriber == null) {
            throw new IllegalArgumentException("参数subscriber不能为空");
        }

        Observable<T> observable = toObservable(type)
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorReturn(new Func1<Throwable, T>() {
                @Override
                public T call(Throwable throwable) {
                    throwable.printStackTrace();
                    return null;
                }
            })
            .subscribeOn(Schedulers.io())
            .filter(new Func1<T, Boolean>() {
                @Override
                public Boolean call(T t) {
                    return t != null;
                }
            })
            .filter(new Func1<T, Boolean>() {
                @Override
                public Boolean call(T t) {
                    return getDefault().toSubscriber == null || subscriber == getDefault().toSubscriber;
                }
            })
            .filter(new Func1<T, Boolean>() {
                @Override
                public Boolean call(T t) {
                    return getDefault().toTag == null || (tag != null && tag.equals(getDefault().toTag));
                }
            });

        switch (threadMode) {
            case MAIN:
                observable = observable.observeOn(AndroidSchedulers.mainThread());
                break;
            case IO:
                observable = observable.observeOn(Schedulers.io());
                break;
            case NEWTHREAD:
                observable = observable.observeOn(Schedulers.newThread());
                break;
            case COMPUTATION:
                observable = observable.observeOn(Schedulers.computation());
                break;
            case TRAMPOLINE:
                observable = observable.observeOn(Schedulers.trampoline());
                break;
            case IMMIDIATE:
            default:
                observable = observable.observeOn(Schedulers.immediate());
        }

        if (!getDefault().subscriptiontMap.containsKey(subscriber)) {
            List<Subscription> subscriptionList = new ArrayList<>();
            subscriptionList.add(observable.subscribe(action));
            getDefault().subscriptiontMap.put(subscriber, subscriptionList);
        } else {
            getDefault().subscriptiontMap.get(subscriber).add(observable.subscribe(action));
        }
    }

    /**
     * 订阅事件
     * @param subscriber 订阅者对象
     * @param type 事件的类型
     * @param threadMode 线程模式，处理事件时所在的线程
     * @param action 事件的处理程序
     */
    public static <T> void subscribe(Object subscriber, Class<T> type, int threadMode, Action1<T> action) {
        subscribe(subscriber, type, threadMode, action, null);
    }

    /**
     * 订阅事件
     * @param subscriber 订阅者对象
     * @param type 事件的类型
     * @param action 事件的处理程序
     * @param tag 标记订阅者的类别
     */
    public static <T> void subscribe(Object subscriber, Class<T> type, Action1<T> action, String tag) {
        subscribe(subscriber, type, IMMIDIATE, action, tag);
    }

    /**
     * 订阅事件
     * @param subscriber 订阅者对象
     * @param type 事件的类型
     * @param action 事件的处理程序
     */
    public static <T> void subscribe(Object subscriber, Class<T> type, Action1<T> action) {
        subscribe(subscriber, type, IMMIDIATE, action, null);
    }

}
