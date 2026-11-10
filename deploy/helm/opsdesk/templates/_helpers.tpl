{{/* Chart name, overridable. */}}
{{- define "opsdesk.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/* Fully qualified name: the release name if it already contains the chart name. */}}
{{- define "opsdesk.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{- define "opsdesk.selectorLabels" -}}
app.kubernetes.io/name: {{ include "opsdesk.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "opsdesk.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
{{ include "opsdesk.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.image.tag | default .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "opsdesk.image" -}}
{{- printf "%s:%s" .Values.image.repository (.Values.image.tag | default .Chart.AppVersion) }}
{{- end }}

{{/* Secret with the DB password: the generated dev one, or an existing one. Never a value from values.yaml. */}}
{{- define "opsdesk.dbSecretName" -}}
{{- if .Values.devDatabase.enabled }}
{{- printf "%s-db" (include "opsdesk.fullname" .) }}
{{- else }}
{{- required "database.existingSecret is required when devDatabase.enabled is false" .Values.database.existingSecret }}
{{- end }}
{{- end }}

{{- define "opsdesk.dbUrl" -}}
{{- if .Values.devDatabase.enabled }}
{{- printf "jdbc:postgresql://%s-db:5432/opsdesk" (include "opsdesk.fullname" .) }}
{{- else }}
{{- required "database.url is required when devDatabase.enabled is false" .Values.database.url }}
{{- end }}
{{- end }}

{{/* Container security context for the OpenShift restricted-v2 SCC and the restricted Pod Security level. */}}
{{- define "opsdesk.containerSecurityContext" -}}
allowPrivilegeEscalation: false
readOnlyRootFilesystem: true
runAsNonRoot: true
capabilities:
  drop: [ALL]
seccompProfile:
  type: RuntimeDefault
{{- end }}
