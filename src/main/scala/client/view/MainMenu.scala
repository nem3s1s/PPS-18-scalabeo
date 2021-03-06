package client.view

import animatefx.animation.{AnimationFX, FadeIn, SlideOutLeft}
import client.controller.Controller
import client.controller.Messages.ViewToClientMessages.{JoinQueue, UserReadyToJoin, UsernameChosen}
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.control.{Button, Label, ProgressIndicator, TextField}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.Includes.{handle, _}
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.layout.{StackPane, VBox}
import scalafx.stage.Stage

/** MainMenu rappresenta il menù principale di gioco.
 *
 */
class MainMenu extends JFXApp.PrimaryStage {
  private val VBOX_CHILDREN_SPACING = 23
  private val VBOX_WIDTH = 400
  private val ICON_PATH = "/assets/start.png"
  private val STYLESHEET_PATH = "/style/Menu.css"
  private val GREETING_MESSAGE = "Benvenuto su Scalabeo"
  private val GREETING_MESSAGE_STYLE = "greetingLabel"
  private val MENU_TEXTFIELD_STYLE = "textfield"
  private val OUTER_VBOX_STYLE = "outerVBox"
  private val USERNAME_LABEL = "username"
  private val LOGIN_BUTTON_LABEL = "Login"
  private val PLAY_BUTTON_LABEL = "Play"
  private val EXIT_BUTTON_LABEL = "Exit"
  private val MENU_BUTTON_STYLE = "menu-button"
  private val INNER_LOGIN_VBOX_STYLE = "innerLoginVBox"
  private val APPLICATION_TITLE = "Scalabeo"
  private val DIALOG_FOUND_TEXT = "Match Found, do you want to accept?"

  title = APPLICATION_TITLE
  icons.add(new Image(this.getClass.getResourceAsStream(ICON_PATH)))

  val loading: ProgressIndicator = new ProgressIndicator() {
    visible = false
  }

  val usernameInputField: TextField = new TextField() {
    styleClass += MENU_TEXTFIELD_STYLE
    promptText = USERNAME_LABEL
    focusTraversable = false
    onKeyPressed = (event: KeyEvent) => {
      event.code match {
        case KeyCode.Enter =>
          loginButton.fire()
        case _ =>
      }
    }
  }

  val loginButton: Button = new Button(LOGIN_BUTTON_LABEL) {
    styleClass += MENU_BUTTON_STYLE
    onAction = handle {
      Platform.runLater(() => {
        usernameInputField.visible = false
        loading.visible = true
        loginButton.disable = true
      })
      Controller.username_=(usernameInputField.getText)
      View.sendToClient(UsernameChosen(usernameInputField.getText))
    }
  }

  val loginInnerContainer: VBox = new VBox(VBOX_CHILDREN_SPACING){
    alignment = Pos.BottomCenter
    minWidth = VBOX_WIDTH
    styleClass += INNER_LOGIN_VBOX_STYLE
    children = List(loading, usernameInputField, loginButton)
  }

  val playButton: Button = new Button (PLAY_BUTTON_LABEL){
    styleClass += MENU_BUTTON_STYLE
    onAction = handle {
      View.sendToClient(JoinQueue())
      startMatchMaking()
    }
  }

  val loggedInContainer: VBox = new VBox(VBOX_CHILDREN_SPACING){
    alignment = Pos.BottomCenter
    minWidth = VBOX_WIDTH
    styleClass += INNER_LOGIN_VBOX_STYLE
    opacity = 0.0
    children = List(
      playButton,
      new Button(EXIT_BUTTON_LABEL) {
        styleClass += MENU_BUTTON_STYLE
        onAction = handle{
          Platform.exit();
          System.exit(0)
        }
      }
    )
  }

  val outerContainer: VBox =  new VBox(VBOX_CHILDREN_SPACING){
    alignment = Pos.TopCenter
    styleClass += OUTER_VBOX_STYLE
    minWidth = VBOX_WIDTH
    children = List(
      new Label(GREETING_MESSAGE) {
        styleClass += GREETING_MESSAGE_STYLE
      },
      new ImageView(new Image(this.getClass.getResource(ICON_PATH).toExternalForm)) {
        alignmentInParent = Pos.BottomCenter
      },
      loginInnerContainer
    )
  }

  val mainPane: StackPane = new StackPane() {
    children = outerContainer
  }

  onCloseRequest = handle {
    View.terminate()
  }

  /** Metodo chiamato quando l'utente effettua con successo il login.
   *
   */
  def onLoginResponse(): Unit = {
    loginAnimation()
  }

  /** Metodo che mostra all'utente una dialog quando viene trovata una partita.
   *
   */
  def askUserToJoinGame(): Unit = {
    Platform.runLater(() => {
      loggedInContainer.children.remove(0)
      sizeToScene()
      new Dialog(DIALOG_FOUND_TEXT)
        .addYesNoButtons(
          () => View.sendToClient(UserReadyToJoin(true)),
          () => {
            View.sendToClient(UserReadyToJoin(false))
            playButton.disable = false
          })
        .show()
    })
  }

  scene = new Scene {
    root = mainPane
    stylesheets_=(List(STYLESHEET_PATH))
  }
  resizable = false
  show()

  private def loginAnimation(): Unit = {
    Platform.runLater(() => {
      loading.visible = false
      val an: AnimationFX =  new SlideOutLeft(loginInnerContainer)
      an.setOnFinished(() => {
        outerContainer.children.remove(loginInnerContainer)
        outerContainer.children.add(loggedInContainer)
        new FadeIn(loggedInContainer).play()
        sizeToScene()
      })
      an.play()
    })
  }

  private def startMatchMaking(): Unit = {
    Platform.runLater(() => {
      playButton.disable = true
      loggedInContainer.children.add(0, loading)
      loading.visible = true
      sizeToScene()
    })
  }
}

