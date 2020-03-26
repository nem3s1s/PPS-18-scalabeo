package server

import shared.Topic.GAME_SERVER_SEND_TOPIC
import server.GreetingToGameServer.InitGame
import akka.actor.{Actor, ActorRef}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import model.{LettersBagImpl, LettersHandImpl}
import shared.ClientMoveAckType.{HandSwitchRequestAccepted, HandSwitchRequestRefused, PassAck, TimeoutAck}
import shared.ClientToGameServerMessages.{ClientMadeMove, MatchTopicListenAck, PlayerTurnBeginAck}
import shared.GameServerToClientMessages.{ClientMoveAck, EndTurnUpdate, MatchTopicListenQuery, PlayerTurnBegins}
import shared.{ClusterScheduler, CustomScheduler, Move}

import scala.collection.mutable

class GameServer(players : List[ActorRef], mapUsername : Map[ActorRef, String]) extends Actor {

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(GAME_SERVER_SEND_TOPIC, self)
  private val cluster = Cluster.get(context.system)
  private val scheduler: CustomScheduler = ClusterScheduler(cluster)
  private val nPlayer = players.size

  private var greetingServerRef: ActorRef = _
  private val gamePlayers = players
  private val gamePlayersUsername: Map[ActorRef, String] = mapUsername

  private val board = ??? //TODO inserire il tabellone del model
  private val pouch = LettersBagImpl()
  private var playersHand = mutable.Map[ActorRef, LettersHandImpl]()
  //creo le mani
  gamePlayers.foreach(p => playersHand+=(p -> LettersHandImpl.apply(mutable.ArrayBuffer(pouch.takeRandomElementFromBagOfLetters(8).get : _*))))
  private var turn = 0

  //variabili ack
  private var ackTopicReceived = 0
  private var ackTurn = 0

  override def receive: Receive = {
    case _: InitGame =>
      scheduler.replaceBehaviourAndStart(() => sendTopic())
      greetingServerRef = sender()
    case _: MatchTopicListenAck =>
      incrementAckTopic()
      if (ackTopicReceived == nPlayer) {
        scheduler.stopTask()
        resetAckTopic()
        scheduler.replaceBehaviourAndStart(() => sendTurn())
        println("STA A" + gamePlayers(turn).toString() + " IL CUI TURNO è = " + turn)
      }
    case _: PlayerTurnBeginAck =>
      incrementAckTurn()
      if (ackTurn == nPlayer) {
        scheduler.stopTask()
        resetAckTurnCounter()
      }
    case message: ClientMadeMove => message.move match {
      case _: Move.Pass =>
        if (sender().equals(gamePlayers(turn))) {
          sender ! ClientMoveAck(PassAck())
          println("IL player " + sender() + " ha passato")
          scheduler.replaceBehaviourAndStart(() => sendUpdate())
        }
      case _: Move.TimeOut =>
        if (sender().equals(gamePlayers(turn))) {
          sender ! ClientMoveAck(TimeoutAck())
          println("IL player " + sender() + " ha fatto passare troppo tempo: TIMEOUT")
          scheduler.replaceBehaviourAndStart(() => sendUpdate())
        }
      case _: Move.Switch =>
        if(sender().equals(gamePlayers(turn))){
          if (playersHand(sender()).containsOnlyVowelsOrOnlyConsonants()){
            //TODO
            sender ! ClientMoveAck(HandSwitchRequestAccepted(playersHand(sender())._hand))
            scheduler.replaceBehaviourAndStart(() => sendUpdate())
          } else {
            sender ! ClientMoveAck(HandSwitchRequestRefused())
          }
        }
      case _: Move.WordMove =>
        if(sender().equals(gamePlayers(turn))){
          //TODO
        }
    }
  }

  //comportamento dello scheduler
  private def sendTopic(): Unit = {
    gamePlayers.foreach(player => player ! MatchTopicListenQuery(GAME_SERVER_SEND_TOPIC, playersHand(player)._hand))
  }

  private def sendTurn(): Unit = {
    mediator ! Publish(GAME_SERVER_SEND_TOPIC, PlayerTurnBegins(gamePlayers(turn)))
  }

  private def sendUpdate(): Unit = {
    mediator ! Publish(GAME_SERVER_SEND_TOPIC, EndTurnUpdate(???))
  }

  //gestione variabili ack
  private def incrementAckTopic(){
    ackTopicReceived = ackTopicReceived + 1
  }
  private def resetAckTopic(): Unit = {
    ackTopicReceived = 0
  }
  private def incrementAckTurn() = {
    ackTurn = ackTurn + 1
  }
  private def resetAckTurnCounter(): Unit = {
    ackTurn = 0
  }
}