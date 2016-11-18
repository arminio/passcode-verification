passcode-verification
====================

Micro library containing authentication passcode provider function


## Tutorial

### Configuration

In ```/conf/play.plugins``` add the reference to ```PasscodeAuthenticationPlugin```

```scala
    4000:uk.gov.hmrc.passcode.authentication.plugin.PasscodeAuthenticationPlugin
```

*NOTE: if 4000 is already in use choose another number*

In ```/conf/application.conf```, enable the passcodeAuthentication and setup the regime name:

```scala
    passcodeAuthentication.enabled=true
    passcodeAuthentication.regime=charities
```

You will need to add a dependency to gov.uk.hmrc.passcode-verification in your build file.

### Usage

1. Extend your controller with the PasscodeAuthentication trait.
2. Wrap your entry point with the PasscodeAuthenticatedActionAsync or PasscodeAuthenticatedAction

    Example:
    
    ```scala
        class MyController extends PasscodeAuthentication {
        ...
          def showPage = PasscodeAuthenticatedActionAsync {
            // here goes your code
            Future.successful(Ok)
          }
    
          def showPage = PasscodeAuthenticatedAction {
          // here goes your code
             Ok
          }
        }
    ```

3. Add a logout button in your view:
    That will render a logout button with a href to the logout page

    Example:
    
    ```scala
       @passcode.authentication.logout_button()
    ```

*NOTE: If you need to override the default configuration for the verification-frontend service , this can be done in the ```/conf/application.conf```*

## Installing
 
Include the following dependency in your SBT build
 
``` scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")
 
libraryDependencies += "uk.gov.hmrc" %% "passcode-verification" % "[INSERT-VERSION]"

