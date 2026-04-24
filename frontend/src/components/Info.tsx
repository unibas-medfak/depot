import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'

const BANNER = String.raw`
            dddddddd
            d::::::d                                                                  tttt
            d::::::d                                                               ttt:::t
            d::::::d                                                               t:::::t
            d:::::d                                                                t:::::t
    ddddddddd:::::d     eeeeeeeeeeee    ppppp   ppppppppp      ooooooooooo   ttttttt:::::ttttttt
  dd::::::::::::::d   ee::::::::::::ee  p::::ppp:::::::::p   oo:::::::::::oo t:::::::::::::::::t
 d::::::::::::::::d  e::::::eeeee:::::eep:::::::::::::::::p o:::::::::::::::ot:::::::::::::::::t
d:::::::ddddd:::::d e::::::e     e:::::epp::::::ppppp::::::po:::::ooooo:::::otttttt:::::::tttttt
d::::::d    d:::::d e:::::::eeeee::::::e p:::::p     p:::::po::::o     o::::o      t:::::t
d:::::d     d:::::d e:::::::::::::::::e  p:::::p     p:::::po::::o     o::::o      t:::::t
d:::::d     d:::::d e::::::eeeeeeeeeee   p:::::p     p:::::po::::o     o::::o      t:::::t
d:::::d     d:::::d e:::::::e            p:::::p    p::::::po::::o     o::::o      t:::::t    tttttt
d::::::ddddd::::::dde::::::::e           p:::::ppppp:::::::po:::::ooooo:::::o      t::::::tttt:::::t
 d:::::::::::::::::d e::::::::eeeeeeee   p::::::::::::::::p o:::::::::::::::o      tt::::::::::::::t
  d:::::::::ddd::::d  ee:::::::::::::e   p::::::::::::::pp   oo:::::::::::oo         tt:::::::::::tt
   ddddddddd   ddddd    eeeeeeeeeeeeee   p::::::pppppppp       ooooooooooo             ttttttttttt
                                         p:::::p
                                         p:::::p
                                        p:::::::p
                                        p:::::::p
                                        p:::::::p
                                        ppppppppp
`

type BootMode = 'type' | 'instant'
type BootEntry = { text: string; mode?: BootMode }
type LineCls = '' | 'term-err' | 'term-hint'
type HistLine = { text: string; cls?: LineCls }
type Phase = 'boot' | 'login' | 'password' | 'checking' | 'done'

const HOST =
  typeof window !== 'undefined' ? window.location.hostname : 'localhost'

const ANCIENT_OSES = [
  'SunOS 5.6 Generic_105181-41 sun4u sparc SUNW,Ultra-5_10',
  'IRIX 6.5.30 IP35',
  'HP-UX B.11.00 A 9000/800',
  'AIX 4.3.3 0 powerpc',
  'Tru64 UNIX V5.1B (Rev. 2650)',
  'Ultrix 4.5 VAX',
  'UNICOS 10.0.0.8',
  'NeXTSTEP 3.3 Intel',
  'SCO OpenServer 5.0.7',
  'BSD/OS 4.3.1',
  'A/UX 3.0.1',
  'Xenix 386 2.3.4',
  'OSF/1 V1.3',
  'DYNIX/ptx 4.6.2',
  'Plan 9 from Bell Labs (4th Edition)',
  'V7 UNIX (PDP-11/70)',
  '4.3BSD-Tahoe',
  'Minix 1.5',
]

const LOADS = [
  '0.000002', '9001.42', '31337.99', '65535.00', '999999.99',
  '-1.00', 'NaN', '1.21e9', '0.00000001',
]
const PROCESSES = [
  '2147483647', '18446744073709551615', '9001', '42', '0', '-1',
  '65536', '3', '0xDEADBEEF',
]
const USERS = ['0', '1', '42', '31337', '9001', '-1', '999999', '0.5']
const MEMORY = ['127%', '420%', '666%', '-3%', '0.01%', '1024%', '3.14%', '110%']
const SWAP = ['404%', '200%', '500%', '0%', '99.9999%', '1337%', 'NaN%', '∞%']
const USAGES = [
  '110.3% of 4.2KB', '99.99% of 1B', '420% of 13.37PB',
  '0.00001% of 0B', '150% of 640KB', '75% of 1.44MB',
  '0% of 9.4608e15m³', '250% of 128 bytes',
]
const IPS = [
  '256.0.0.1', '999.999.999.999', '127.0.0.42',
  '8.8.8.42069', '255.255.255.256', '13.37.13.37',
  '0xC0.0xFF.0xEE.0x42',
]
const IFACES = ['eth0', 'eth666', 'ens192', 'lo9001', 'qe0', 'le0', 'ppp0', 'hme0']

function pick<T>(arr: T[]): T {
  return arr[Math.floor(Math.random() * arr.length)]
}

function formatMotdDate(d: Date): string {
  const wd = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][d.getDay()]
  const mo = [
    'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
  ][d.getMonth()]
  const day = String(d.getDate()).padStart(2, '0')
  let h = d.getHours()
  const ampm = h >= 12 ? 'PM' : 'AM'
  h = h % 12 || 12
  const hh = String(h).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  let tz = 'UTC'
  try {
    const parts = Intl.DateTimeFormat('en-US', {
      timeZoneName: 'short',
    }).formatToParts(d)
    tz = parts.find((p) => p.type === 'timeZoneName')?.value ?? 'UTC'
  } catch {
    /* keep UTC fallback */
  }
  return `${wd} ${mo} ${day} ${hh}:${mm}:${ss} ${ampm} ${tz} ${d.getFullYear()}`
}

function motdLine(k1: string, v1: string, k2 = '', v2 = ''): string {
  let s = k1.padEnd(14) + v1
  if (k2) {
    s = s.length < 33 ? s.padEnd(33) : s + '  '
    s += k2.padEnd(25) + v2
  }
  return s
}

function buildBoot(): BootEntry[] {
  const os = pick(ANCIENT_OSES)
  const iface = pick(IFACES)
  const ip = pick(IPS)
  const date = formatMotdDate(new Date())
  const inst = (text: string): BootEntry => ({ text, mode: 'instant' })
  return [
    { text: `Trying ${pick(IPS)}...` },
    { text: `Connected to ${HOST}.` },
    { text: `Escape character is '^]'.` },
    { text: '' },
    inst(`Welcome to ${os}`),
    inst(''),
    inst(`System information as of ${date}`),
    inst(''),
    inst(motdLine('System load:', pick(LOADS), 'Processes:', pick(PROCESSES))),
    inst(motdLine('Usage of /:', pick(USAGES), 'Users logged in:', pick(USERS))),
    inst(motdLine('Memory usage:', pick(MEMORY), `IPv4 address for ${iface}:`, ip)),
    inst(motdLine('Swap usage:', pick(SWAP))),
    inst(''),
  ]
}

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms))

export function Info() {
  const [history, setHistory] = useState<HistLine[]>([])
  const [typing, setTyping] = useState('')
  const [phase, setPhase] = useState<Phase>('boot')
  const [login, setLogin] = useState('')
  const [password, setPassword] = useState('')
  const [blink, setBlink] = useState(true)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    const id = setInterval(() => setBlink((b) => !b), 500)
    return () => clearInterval(id)
  }, [])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const entries = buildBoot()
      for (const entry of entries) {
        if (cancelled) return
        if (entry.text === '') {
          setHistory((h) => [...h, { text: '' }])
          await sleep(entry.mode === 'instant' ? 50 : 220)
          continue
        }
        if (entry.mode === 'instant') {
          setHistory((h) => [...h, { text: entry.text }])
          await sleep(70 + Math.random() * 60)
          continue
        }
        let pos = 0
        while (pos < entry.text.length) {
          if (cancelled) return
          pos = Math.min(
            pos + 1 + Math.floor(Math.random() * 3),
            entry.text.length,
          )
          setTyping(entry.text.slice(0, pos))
          await sleep(18 + Math.random() * 70)
        }
        if (cancelled) return
        setHistory((h) => [...h, { text: entry.text }])
        setTyping('')
        await sleep(180 + Math.random() * 320)
      }
      if (cancelled) return
      setPhase('login')
    })()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (phase === 'login' || phase === 'password') inputRef.current?.focus()
  }, [phase])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (phase === 'login') {
      if (!login) return
      setHistory((h) => [...h, { text: `depot login: ${login}` }])
      setLogin('')
      setPhase('password')
    } else if (phase === 'password') {
      setHistory((h) => [...h, { text: 'Password: ' }])
      setPassword('')
      setPhase('checking')
      await sleep(900 + Math.random() * 600)
      setHistory((h) => [
        ...h,
        { text: 'Login incorrect', cls: 'term-err' },
        { text: '' },
        { text: 'Hint: ask your realm admin for a signed URL.', cls: 'term-hint' },
      ])
      setPhase('done')
    }
  }

  const cursor = blink ? '█' : ' '

  return (
    <div className="info">
      <pre className="info-banner" aria-label="depot">{BANNER}</pre>
      <div
        className="info-terminal"
        onClick={() => inputRef.current?.focus()}
      >
        {history.map((l, i) => (
          <div key={i} className={`term-line ${l.cls ?? ''}`}>
            {l.text || ' '}
          </div>
        ))}
        {typing && (
          <div className="term-line">
            {typing}
            <span className="term-cursor">{cursor}</span>
          </div>
        )}
        {phase === 'login' && (
          <div className="term-line">
            depot login: {login}
            <span className="term-cursor">{cursor}</span>
          </div>
        )}
        {phase === 'password' && (
          <div className="term-line">
            Password: <span className="term-cursor">{cursor}</span>
          </div>
        )}
        {phase === 'checking' && (
          <div className="term-line">
            <span className="term-cursor">{cursor}</span>
          </div>
        )}
        <form onSubmit={handleSubmit} className="term-form" autoComplete="off">
          <input
            ref={inputRef}
            className="term-input"
            type="text"
            name="tty"
            value={phase === 'password' ? password : login}
            onChange={(e) =>
              phase === 'password'
                ? setPassword(e.target.value)
                : setLogin(e.target.value)
            }
            disabled={phase !== 'login' && phase !== 'password'}
            autoComplete="off"
            autoCapitalize="off"
            autoCorrect="off"
            spellCheck={false}
            aria-label="terminal input"
          />
        </form>
      </div>
    </div>
  )
}
