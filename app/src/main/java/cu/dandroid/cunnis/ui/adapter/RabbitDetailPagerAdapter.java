package cu.dandroid.cunnis.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import cu.dandroid.cunnis.ui.fragment.*;

public class RabbitDetailPagerAdapter extends FragmentStateAdapter {
    public RabbitDetailPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new RabbitProfileFragment();
            case 1: return new WeightHistoryFragment();
            case 2: return new HealthHistoryFragment();
            case 3: return new ReproductiveHistoryFragment();
            default: return new RabbitProfileFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
