package com.jaba.awr_3.inits;



import com.jaba.awr_3.core.connectors.ComService;
import com.jaba.awr_3.core.connectors.TcpService;
import com.jaba.awr_3.core.sysutils.SysIdService;
import com.jaba.awr_3.core.units.UnitService;
import com.jaba.awr_3.inits.ffmpeg.FfmpegInitializer;
import com.jaba.awr_3.inits.fonts.FontsInstaller;
import com.jaba.awr_3.inits.postgres.DataService;
import com.jaba.awr_3.inits.postgres.PostgresInit;
import com.jaba.awr_3.inits.repo.RepoInit;
import com.jaba.awr_3.seversettings.basic.BasicService;
import com.jaba.awr_3.seversettings.network.DefaultNetwork;
import com.jaba.awr_3.seversettings.owner.OwnerService;

public class MainInit {
    public static void initAll() {
        RepoInit.initRepos();
        DefaultNetwork.createDefaultNetPlan();
        BasicService.initBasicSettings();
        UnitService.initUnitS();
        DataService.initDataSettings();
        OwnerService.initOwnerSettings();
        PostgresInit.init();
        FfmpegInitializer.init();
        FontsInstaller.init();
        SysIdService.init();
        ComService.initComPorts();
        TcpService.initTcp();
    }
}
