package com.onyx.decoled;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class DecoTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        LedController controller = new LedController(this);
        boolean newState = !controller.isMasterEnabled();
        controller.setMasterEnabled(newState);
        controller.applyState();
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        LedController controller = new LedController(this);
        boolean enabled = controller.isMasterEnabled();

        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.tile_name));
        tile.setSubtitle(enabled ? "已开启" : "已关闭");
        tile.updateTile();
    }
}
