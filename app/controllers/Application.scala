package controllers

import java.io.File

import play.api._
import play.api.mvc._
import play.api.Play.current
import slick.driver.JdbcProfile
import play.api.db.slick._



class Application extends Controller {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)


  def index = Action { request =>
    Logger.info("Serving index page...")
    request.getQueryString("subDomain") match {
      case Some(subDomain) => Ok(views.html.index(subDomain))
      case None => Ok(views.html.index("en.wikipedia"))
    }

  }

  private def findScripts(base: File): Seq[String] = {
    val baseUri = base.toURI
    directoryFlatMap(base, scriptMapper).
      map(f => baseUri.relativize(f.toURI).getPath)
  }

  private def scriptMapper(file: File): Option[File] = {
    val name = file.getName
    if (name.endsWith(".js")) Some(file)
    else if (name.endsWith(".coffee")) Some(new File(file.getParent, name.dropRight(6) + "js"))
    else None
  }

  private def directoryFlatMap[A](in: File, fileFun: File => Option[A]): Seq[A] = {
    in.listFiles.flatMap {
      case f if f.isDirectory => directoryFlatMap(f, fileFun)
      case f if f.isFile => fileFun(f)
    }
  }

}
