package gtc.dcc.put0.core.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import gtc.dcc.put0.core.view.AccountActivity;
import gtc.dcc.put0.core.view.GameActivity;
import gtc.dcc.put0.core.view.MainActivity;
import gtc.dcc.put0.core.view.SettingsActivity;
import gtc.dcc.put0.core.view.TestActivity;


public class MainViewModel extends ViewModel {

    private final MutableLiveData<Class<?>> _selectedActivity = new MutableLiveData<>();
    public final LiveData<Class<?>> selectedActivity = _selectedActivity;

    private final MutableLiveData<String> _userName = new MutableLiveData<>();
    public final LiveData<String> userName = _userName;

    public void setUserName(String name) {
        _userName.setValue(name);
    }

    public void onCreateGameClicked() {
        _selectedActivity.setValue(MainActivity.class);
    }

    public void onJoinGameClicked() {
        _selectedActivity.setValue(GameActivity.class);
    }

    public void onSettingsClicked() {
        _selectedActivity.setValue(SettingsActivity.class);
    }

    public void onAddFriendsClicked() {
        _selectedActivity.setValue(TestActivity.class);
    }

    public void onLogoutClicked() {
        _selectedActivity.setValue(SettingsActivity.class);
    }
}
