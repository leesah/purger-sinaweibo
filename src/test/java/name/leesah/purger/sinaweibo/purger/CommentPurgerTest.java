package name.leesah.purger.sinaweibo.purger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import name.leesah.purger.sinaweibo.purger.tools.UniversalListener;
import name.leesah.sinaweiboapi.ListingRequest;
import name.leesah.sinaweiboapi.comment.ListComments;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by sah on 2017-04-14.
 */
@RunWith(RobolectricTestRunner.class)
public class CommentPurgerTest {
    private CommentPurger purger;

    @Mock
    private
    UniversalListener listener;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        purger = new CommentPurger();
    }

    @Test
    public void buildListingRequest() throws Exception {

        ListingRequest listingRequest = purger.buildListingRequest("some_appkey", "some_uid", "some_token", listener, listener);
        assertThat(listingRequest, is(equalTo(new ListComments("some_appkey", "some_uid", "some_token", listener, listener))));
    }

    @Test
    public void getIdsFromListingResponseLessThan20() throws Exception {
    }

    @Test
    public void buildDestroyingRequest() throws Exception {
    }

}