package me.knighthat.internal.util

  import me.knighthat.innertube.Innertube
  import me.knighthat.innertube.request.Localization
  import me.knighthat.innertube.request.body.Context
  import me.knighthat.internal.InnertubeImpl
  import org.jetbrains.annotations.Contract


  /**
   * Generate **new** [Context] from provided [template].
   *
   * If [useLogin] is `true` then [Innertube.KtorProvider.visitorData]
   * is used along with other properties.
   *
   * On API 24 (Android 7) devices the visitor-data token cannot be fetched
   * from the network at login time, so [Innertube.KtorProvider.visitorData]
   * may be blank.  YouTube accepts a blank visitorData for anonymous requests
   * but returns HTTP 400 INVALID_ARGUMENT for authenticated ones.  We therefore
   * fall back to the template's hard-coded constant whenever the account token
   * is absent. The same guard applies to [Innertube.KtorProvider.dataSyncId]:
   * an empty string is serialised as `"onBehalfOfUser":""` which YouTube also
   * rejects, so we treat it the same as null.
   */
  @Contract(
      value = "->new",
      pure = true
  )
  internal fun InnertubeImpl.getContext(
      template: Context = Context.WEB_REMIX_DEFAULT,
      localization: Localization = Localization.EN_US,
      visitorData: String? = null,
      useLogin: Boolean = false
  ): Context {
      val resolvedVisitorData = if( useLogin )
          // Use account visitorData when available; fall back to template's hardcoded constant
          // so the player request is never sent with a blank value (causes HTTP 400 on API 24+
          // when an authenticated SAPISID cookie is also present).
          provider.visitorData.ifBlank { template.client.visitorData }
      else
          // Prefer the caller-supplied token (fresh or cached); fall back to the template's
          // hardcoded constant.  The old fallback was template.client.userAgent which is a
          // plain user-agent string, not a valid base-64 protobuf visitor-data token.
          visitorData ?: template.client.visitorData

      // An empty dataSyncId serialises as "onBehalfOfUser":"" which YouTube rejects.
      // Treat blank the same as absent so the field is omitted from the JSON body.
      val resolvedDataSyncId = provider.dataSyncId?.takeIf { it.isNotBlank() }

      return Context(
          template.client.copy(
              hl = localization.languageCode,
              gl = localization.regionCode,
              visitorData = resolvedVisitorData
          ),
          Context.User().copy(
              onBehalfOfUser = if( useLogin ) resolvedDataSyncId else null
          )
      )
  }
  