package com.jaba.awr_3.mvc;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jaba.awr_3.inits.repo.RepoInit;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {

                registry
                                .addResourceHandler("/backup/**")
                                .addResourceLocations("file:" + RepoInit.BACKUP_REPO_MVC.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/logo/**")
                                .addResourceLocations("file:" + RepoInit.LOGO_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/archipdf/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_FULL.getAbsolutePath() + "/")
                                .setCachePeriod(0);

                registry
                                .addResourceHandler("/pdflast_0/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_LAST_0.getAbsolutePath() + "/")
                                .setCachePeriod(0);

                registry
                                .addResourceHandler("/pdflast_1/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_LAST_1.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/pdflast_2/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_LAST_2.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/pdflast_3/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_LAST_3.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/pdflast_4/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_LAST_4.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/pdflast_5/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_LAST_5.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/pdflast_6/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_LAST_6.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/pdflast_7/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_LAST_7.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/pdflast_8/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_LAST_8.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/pdflast_9/**")
                                .addResourceLocations("file:" + RepoInit.PDF_REPOSITOR_LAST_9.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/videoarchive/**")
                                .addResourceLocations("file:" + RepoInit.VIDEO_ARCHIVE.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/cam0/**")
                                .addResourceLocations("file:" + RepoInit.CAM_0_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/cam1/**")
                                .addResourceLocations("file:" + RepoInit.CAM_1_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/cam2/**")
                                .addResourceLocations("file:" + RepoInit.CAM_2_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/cam3/**")
                                .addResourceLocations("file:" + RepoInit.CAM_3_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/cam4/**")
                                .addResourceLocations("file:" + RepoInit.CAM_4_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/cam5/**")
                                .addResourceLocations("file:" + RepoInit.CAM_5_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/cam6/**")
                                .addResourceLocations("file:" + RepoInit.CAM_6_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/cam7/**")
                                .addResourceLocations("file:" + RepoInit.CAM_7_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/cam8/**")
                                .addResourceLocations("file:" + RepoInit.CAM_8_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);
                registry
                                .addResourceHandler("/cam9/**")
                                .addResourceLocations("file:" + RepoInit.CAM_9_REPO.getAbsolutePath() + "/")
                                .setCachePeriod(0);

        }

        @Override
        public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                                .allowedOrigins("*")
                                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                                .allowedHeaders("*");
        }

}
