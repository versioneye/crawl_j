package versioneye.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import versioneye.domain.GlobalSetting;
import versioneye.domain.Repository;

@Mojo( name = "repo1html", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class Repo1HtmlMojo extends HtmlMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        try{
            super.execute();

            username = null;
            password = null;

            mavenRepository = mavenRepositoryDao.findByName("custom");
            mavenRepository.setUrl(fetchBaseUrl());
            Repository repository = repositoryUtils.convertRepository(mavenRepository);

            mavenProjectProcessor.setRepository(repository);
            mavenPomProcessor.setRepository(repository);

            addRepo(mavenRepository);

            crawl();
        } catch( Exception exception ){
            getLog().error(exception);
            throw new MojoExecutionException("Oh no! Something went wrong. Get in touch with the VersionEye guys and give them feedback.", exception);
        }
    }

    private String fetchBaseUrl(){
        String env = System.getenv("RAILS_ENV");
        getLog().info("fetchBaseUrl for env: " + env );
        try{
            GlobalSetting gs = globalSettingDao.getBy(env, "mvn_repo_1");
            String url = gs.getValue();
            getLog().info(" - mvn_repo_1: " + url);
            return url;
        } catch( Exception ex){
            ex.printStackTrace();
            return "";
        }
    }

}