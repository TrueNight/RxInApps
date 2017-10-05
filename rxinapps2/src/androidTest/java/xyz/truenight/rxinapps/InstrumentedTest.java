package xyz.truenight.rxinapps;

import android.os.Looper;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.UiThreadTest;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import xyz.truenight.rxinapps.model.ProductType;
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

        final RxInApps rxInApps = RxInApps.with(activityTestRule.getActivity());
        Single<IInAppBillingService> initialization = rxInApps.initialization();

        initialization.subscribe(new Consumer<IInAppBillingService>() {
            @Override
            public void accept(IInAppBillingService iInAppBillingService) throws Exception {
                Log.d("RxInApps", "Sub 1 connected " + iInAppBillingService + "; mainThread = " + isMainThread());
            }
        });
        initialization.subscribe(new Consumer<IInAppBillingService>() {
            @Override
            public void accept(IInAppBillingService iInAppBillingService) throws Exception {
                Log.d("RxInApps", "Sub 2 connected " + iInAppBillingService + "; mainThread = " + isMainThread());
            }
        });

        rxInApps.initialization()
                .flatMap(new Function<IInAppBillingService, SingleSource<? extends List<Purchase>>>() {
                    @Override
                    public SingleSource<? extends List<Purchase>> apply(IInAppBillingService billingService) throws Exception {
                        Log.d("RxInApps", "Sub 3 connected " + billingService + "; mainThread = " + isMainThread());
                        return rxInApps.loadPurchasesByType(billingService, ProductType.MANAGED);
                    }
                })
                .subscribe(new Consumer<List<Purchase>>() {
                    @Override
                    public void accept(List<Purchase> purchases) throws Exception {
                        Log.d("RxInApps", "Sub 3 connected " + purchases.size() + "; mainThread = " + isMainThread());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.d("RxInApps", "Sub 3 error", throwable);
                    }
                });
    }

    private boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}
