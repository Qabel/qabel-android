package de.qabel.qabelbox.ui.views;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.qabel.qabelbox.R;

public class DrawerNavigationViewHolder {
    @BindView(R.id.imageViewExpandIdentity) public ImageView imageViewExpandIdentity;
    @BindView(R.id.textViewSelectedIdentity) public TextView textViewSelectedIdentity;
    @BindView(R.id.accountName) public TextView textViewBoxAccountName;
    @BindView(R.id.qabelLogo) public View qabelLogo;
    @BindView(R.id.select_identity_layout) public View selectIdentityLayout;

    public DrawerNavigationViewHolder(View view) {
        ButterKnife.bind(this, view);
    }
}
