package cu.dandroid.cunnis.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import cu.dandroid.cunnis.ui.fragment.EstrusListFragment;
import cu.dandroid.cunnis.ui.fragment.MatingListFragment;
import cu.dandroid.cunnis.ui.fragment.ParturitionListFragment;

public class ReproductivePagerAdapter extends FragmentStateAdapter {
    private final long rabbitId;

    public ReproductivePagerAdapter(@NonNull FragmentActivity fa, long rabbitId) {
        super(fa);
        this.rabbitId = rabbitId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0: fragment = new EstrusListFragment(); break;
            case 1: fragment = new MatingListFragment(); break;
            case 2: fragment = new ParturitionListFragment(); break;
            default: fragment = new Fragment(); break;
        }
        Bundle args = new Bundle();
        args.putLong("rabbit_id", rabbitId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
