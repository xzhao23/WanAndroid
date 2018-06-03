package com.example.lengary_l.wanandroid.data.source.remote;

import android.support.annotation.NonNull;
import android.util.Log;

import com.example.lengary_l.wanandroid.data.ArticleDetailData;
import com.example.lengary_l.wanandroid.data.ArticlesData;
import com.example.lengary_l.wanandroid.data.source.ArticlesDataSource;
import com.example.lengary_l.wanandroid.realm.RealmHelper;
import com.example.lengary_l.wanandroid.retrofit.RetrofitClient;
import com.example.lengary_l.wanandroid.retrofit.RetrofitService;

import java.util.Comparator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class ArticlesDataRemoteSource implements ArticlesDataSource {
    @NonNull
    public static ArticlesDataRemoteSource INSTANCE;
    private static final String TAG = "ArticlesDataRemoteSourc";




    private ArticlesDataRemoteSource(){

    }

    public static ArticlesDataRemoteSource getInstance(){
        if (INSTANCE == null) {
            INSTANCE = new ArticlesDataRemoteSource();
        }
        return INSTANCE;
    }


    @Override
    public Observable<List<ArticleDetailData>> getArticles(@NonNull final int page, boolean forceUpdate, boolean clearCache) {
        return RetrofitClient.getInstance()
                .create(RetrofitService.class)
                .getArticles(page)
                .filter(new Predicate<ArticlesData>() {
                    @Override
                    public boolean test(ArticlesData articlesData) throws Exception {
                        return articlesData.getErrorCode() != -1&&!articlesData.getData().isOver();
                    }
                })
                .flatMap(new Function<ArticlesData, ObservableSource<List<ArticleDetailData>>>() {
                    @Override
                    public ObservableSource<List<ArticleDetailData>> apply(ArticlesData articlesData) throws Exception {
                        return Observable.fromIterable(articlesData.getData().getDatas()).toSortedList(new Comparator<ArticleDetailData>() {
                            @Override
                            public int compare(ArticleDetailData articleDetailData, ArticleDetailData t1) {
                                if (articleDetailData.getId() > t1.getId()){
                                    return -1;
                                }else {
                                    return 1;
                                }
                            }
                        }).toObservable().doOnNext(new Consumer<List<ArticleDetailData>>() {
                            @Override
                            public void accept(List<ArticleDetailData> list) throws Exception {



                                /*for (ArticleDetailData item :list){
                                    saveToRealm(item, page);
                                }*/
                            }
                        });
                    }
                });
    }

    private void saveToRealm(ArticleDetailData article,int page){
        if (article==null){
            Log.e(TAG, "saveToRealm: article is null" );
        }
        int id = article.getId();
        Realm realm = Realm.getInstance(new RealmConfiguration.Builder()
                .name(RealmHelper.DATABASE_NAME)
                .deleteRealmIfMigrationNeeded()
                .build());
        ArticleDetailData a=realm.copyFromRealm(realm.where(ArticleDetailData.class)
                .equalTo("id", id).findFirst());
        if (a==null){
            article.setCurrentPage(page);
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(article);
            realm.commitTransaction();
        }else {
            a.setCurrentPage(page);
            a.setNiceDate(article.getNiceDate());
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(a);
            realm.commitTransaction();
        }
        realm.close();
    }


}
