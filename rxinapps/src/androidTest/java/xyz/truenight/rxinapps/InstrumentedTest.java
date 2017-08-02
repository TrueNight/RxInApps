package xyz.truenight.rxinapps;

import android.os.Looper;
import android.support.test.annotation.UiThreadTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import xyz.truenight.rxinapps.model.Purchase;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class InstrumentedTest {

    @Rule
    public ActivityTestRule<EmptyActivity> activityTestRule = new ActivityTestRule<>(EmptyActivity.class);

    @Test
    @UiThreadTest
    public void testUseAppContext() throws Exception {
        // Context of the app under test.
//        Context appContext = InstrumentationRegistry.getTargetContext();

        RxInApps rxInApps = RxInApps.with(activityTestRule.getActivity());
        Observable<IInAppBillingService> initialization = rxInApps.initialization();

        initialization.subscribe(new Action1<IInAppBillingService>() {
            @Override
            public void call(IInAppBillingService iInAppBillingService) {
                Log.d("RxInApps", "Sub 1 connected " + iInAppBillingService + "; mainThread = " + isMainThread());
            }
        });
        initialization.subscribeOn(Schedulers.io()).subscribe(new Action1<IInAppBillingService>() {
            @Override
            public void call(IInAppBillingService iInAppBillingService) {
                Log.d("RxInApps", "Sub 2 connected " + iInAppBillingService + "; mainThread = " + isMainThread());
            }
        });

        rxInApps.loadPurchasedProducts().subscribe(new Action1<List<Purchase>>() {
            @Override
            public void call(List<Purchase> purchases) {
                Log.d("RxInApps", "Sub 3 connected " + purchases.size() + "; mainThread = " + isMainThread());
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.d("RxInApps", "Sub 3 error", throwable);
            }
        });
    }

    private boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}
