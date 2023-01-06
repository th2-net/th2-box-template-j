# Template project for th2 component

The project contains the base structure that is required for creation a th2 component.

The minimal set of the required and useful dependencies is added to the `build.gradle` file.

# What do you need to change?

If you are using this template for creating your own component please do the following steps before starting the actual development:
+ Change the **rootProject.name** in `settings.gradle` file. The name **should not** contain the **th2** prefix;
+ Change the **APP_NAME** in the `.gitlab-ci.yml` file. It should be the same as project name but with **th2** prefix;
+ Change the value for **DOCKER_PUBLISH_ENABLED** in the `.gitlab-ci.yml` file to enable docker image publication;
+ Change the package name from `template` to a different name. It probably should be the same as the component name;
+ Correct the following block in the `build.gradle` file according to the previous step
    ```groovy
    application {
        mainClass.set('com.exactpro.th2.template.Main')
    }
    ```

# Useful links

+ th2-common - https://github.com/th2-net/th2-common-j